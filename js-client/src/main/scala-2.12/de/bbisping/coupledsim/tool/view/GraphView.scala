package de.bbisping.coupledsim.tool.view

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.UndefOr.any2undefOrA
import scala.scalajs.js.UndefOr.undefOr2ops
import org.singlespaced.d3js.Link
import org.singlespaced.d3js.forceModule.Node
import de.bbisping.coupledsim.tool.control.Structure
import de.bbisping.coupledsim.tool.model.NodeID

object GraphView {
  var graphBending = false
  
  private val dummyNode = new GraphNode(NodeID("#dummy#"), Structure.NodeLabel(Set(), Option(0), Option(0)))

  trait Linkable {
    def centerX: Double
    def centerY: Double

    def sameRep(a: Linkable): Boolean

    def hasRep(a: Any): Boolean
  }
  class GraphNode(
      var nameId: NodeID,
      var meta: Structure.NodeLabel)
    extends Node with Linkable {
    
    GraphNode.count = GraphNode.count + 1
    
    var fixedPermanently = meta.x.isDefined && meta.y.isDefined
    
    var id: Double = GraphNode.count
    
    var selected = false
    var previouslySelected = false
    
    x = 100 + Math.cos(GraphNode.count * 5.1) * 100
    y = 100 + Math.sin(GraphNode.count * 5.1) * 100
    weight = 1.0
    
    updateMeta(meta, true)
    
    def updateMeta(metaInfo: Structure.NodeLabel, force: Boolean = false) = {
      if (meta != metaInfo || force) {
        meta = metaInfo
        x = UndefOr.any2undefOrA(meta.x getOrElse x.get)
        y = UndefOr.any2undefOrA(meta.y getOrElse y.get)
        px = x
        py = y
        fixedPermanently = meta.x.isDefined && meta.y.isDefined
        fixed = if (fixedPermanently) 1 else 0
      }
    }
    
    def updatePos() = {
      if (fixedPermanently && fixed.get <= 1.5) {
        fixed = 1
        for (tarX <- meta.x; currX <- x.toOption) {
          val xDiff = tarX - currX
          if (Math.abs(xDiff) < 2.0) {
            x = UndefOr.any2undefOrA(tarX)
          } else {
            x = UndefOr.any2undefOrA(currX + 2.0 * Math.signum(xDiff))
            fixed = 0
          }
        }
        for (tarY <- meta.y; currY <- y.toOption) {
          val yDiff = tarY - currY
          if (Math.abs(yDiff) < 2.0) {
            y = UndefOr.any2undefOrA(tarY)
          } else {
            y = UndefOr.any2undefOrA(currY + 2.0 * Math.signum(yDiff))
            fixed = 0
          }
        }
      }
    }
    
    override def sameRep(a: Linkable) = a match {
      case o: GraphNode =>
        o.nameId equals nameId
      case _ =>
        false
    }

    override def hasRep(a: Any) = (nameId == a)
    
    override def centerX = x.getOrElse(0)
    override def centerY = y.getOrElse(0)

    override def hashCode = nameId.hashCode
    
    override def toString = id + nameId.name
  }
  
  object GraphNode {
    var count: Double = 0.0
  }
    
  class NodeLink(
      var kind: Symbol, 
      var label: String,
      var sources: Set[Linkable],
      var targets: Set[Linkable],
      var rep: Any)
    extends Link[GraphNode] with Linkable {
    
    val source = sources.collect { case gn: GraphNode => gn }.headOption.getOrElse(dummyNode)

    val target = targets.collect { case gn: GraphNode => gn }.headOption.getOrElse(dummyNode)

    var length: Double = 0
    
    var dir: (Double, Double) = (0,0)

    val isLoop = sources == targets
    
    var srcCenter: (Double, Double) = if (sources.nonEmpty) (
      (sources.map(_.centerX).sum / sources.size),
      (sources.map(_.centerY).sum / sources.size)
    ) else (0, 0)

    var tarCenter: (Double, Double) = if (targets.nonEmpty) (
      (targets.map(_.centerX).sum / targets.size),
      (targets.map(_.centerY).sum / targets.size)
    ) else (srcCenter._1 + 100, srcCenter._2 + 100)
    
    override def centerX = ((tarCenter._1 + srcCenter._1) / 2) - 20 * dir._2
    override def centerY = (tarCenter._2 + srcCenter._2) / 2 + 20 * dir._1

    val viewParts = {
      sources.map(new LinkViewPart(this, _, isEnd = false)) ++
      targets.map(new LinkViewPart(this, _))
    }

    def updateDirAndCenter() {
      /*if (source.nameId == target.nameId) {
        // loop edge!
        length = 30
        dir = (-1,0)
        center = (target.x.get + 20 * 1.5, target.y.get + 20 * 1.5)
      } else {
        length = Math.hypot(target.x.get - source.x.get, target.y.get - source.y.get)
        dir = ((target.x.get - source.x.get) / length, (target.y.get - source.y.get) / length)
        center = ((target.x.get + source.x.get) / 2, (target.y.get + source.y.get) / 2)
      }*/
      srcCenter = if (sources.nonEmpty) (
        (sources.map(_.centerX).sum / sources.size),
        (sources.map(_.centerY).sum / sources.size)
      ) else (tarCenter._1 + 100, tarCenter._2 + 100)
      tarCenter = if (targets.nonEmpty) (
        (targets.map(_.centerX).sum / targets.size),
        (targets.map(_.centerY).sum / targets.size)
      ) else (
        srcCenter._1 + 100,
        srcCenter._2 + 100
      )
      length = Math.hypot(tarCenter._1 - srcCenter._1, tarCenter._2 - srcCenter._2)
      dir = if (isLoop || length <= 0.0001) (
        (1,0)
      ) else (
        (tarCenter._1 - srcCenter._1) / length,
        (tarCenter._2 - srcCenter._2) / length
      )
    }

    def integrate(nodes: Iterable[Linkable]): Option[NodeLink] = {
      val newSrc = sources.flatMap { n => nodes.find(n.sameRep(_)) }
      val newTar = targets.flatMap { n => nodes.find(n.sameRep(_)) }
      if (newSrc.nonEmpty && newTar.nonEmpty) {
        Some(new NodeLink(kind, label, newSrc, newTar, (label, newSrc, newTar)))
      } else {
        None
      }
    }
    
    def toSVGPathString = (
      ""
      /*if (source == target) { // loop
       "M"   + (source.x.get + 9) + " " + (source.y.get) +
       "A 20 20, 0, 1, 1, " + (target.x.get) + " " + (target.y.get + 9)
      } else { // line
       "M"   + (source.x.get + 9 * dir._1) + " " + (source.y.get + 9 * dir._2) +
       "L"   + (target.x.get - 9 * dir._1) + " " + (target.y.get - 9 * dir._2)
      }*/
    )
    
    override def toString = sources.toString + "-" + kind + "-" + label + "-" + targets.toString
    
    override def hashCode = 23 * kind.hashCode + 39 * sources.hashCode + targets.hashCode
    
    def sameRep(l: Linkable) = l match {
      case o: NodeLink =>
        (o.kind equals kind) &&
          o.label == label &&
          o.sources.forall(s => targets.exists(_.sameRep(s))) &&
          o.targets.forall(t => sources.exists(_.sameRep(t)))
      case _ =>
        false
    }

    override def hasRep(a: Any) = rep == a
  }

  /*class LinkLink(
      val kind: Symbol,
      var source: List[GraphNode],
      var target: LinkTrait)
    extends LinkTrait {
    
    def integrate(nodes: Iterable[GraphNode], links: Iterable[LinkTrait]): Option[LinkLink] = {
      val newSrc = source.flatMap(s => nodes.find(s.sameNode(_)))
      for (
        newTar <- links.find(target.sameLink(_))
      ) yield new LinkLink(kind, newSrc, newTar)
    }
    
    var srcCenter: (Double, Double) = (
      (source.map(_.x.get).sum / source.length),
      (source.map(_.y.get).sum / source.length)
    )
    
    var center: (Double, Double) = (
      (target.center._1 + srcCenter._1) / 2 + 10.0,
      (target.center._2 + srcCenter._2) / 2
    )
    
    updatePos()
    
    def updatePos() = {
      srcCenter = (
        (source.map(_.x.get).sum / source.length),
        (source.map(_.y.get).sum / source.length)
      )
      center = (
        (target.center._1 + srcCenter._1) / 2,
        (target.center._2 + srcCenter._2) / 2
      )
      // bending of higher order arrows
      if (graphBending) {
        val dx = target.center._1 - srcCenter._1
        val dy = target.center._2 - srcCenter._2
        val lengthInv = 25.0 / Math.sqrt(dx * dx + dy * dy)
        center = (
          center._1 - dy * lengthInv,
          center._2 + dx * lengthInv
        )
      }
    }
    
    def sameLink(l: LinkTrait) = l match {
      case o: LinkLink =>
        (o.kind equals kind) &&
        o.source.map(_.esEvent).toSet == source.map(_.esEvent).toSet &&
        o.target.sameLink(target)
      case _ =>
        false
    }
    
    def matchesRule(r: HDEventStructure.Rule): Boolean = {
      savedRule.exists(_ == r)
    }
    
    override def toString = source.toString + "-" + kind + "-" + target.toString
  }*/
  
  class LinkViewPart(val link: NodeLink, val source: Linkable, val isEnd: Boolean = true) {
    
    def toSVGPathString = {
      if (link.tarCenter._1.isNaN()) throw new Exception("NaN tar!")
      if (link.dir._1.isNaN()) throw new Exception("NaN dir!")
      if (link.source.centerX.isNaN()) throw new Exception("NaN center!")
      if (isEnd) {
        "M"   + link.centerX       +" "+ link.centerY + 
            " C " + (link.centerX + .3 * link.length * link.dir._1) +" "+ (link.centerY + .3 * link.length * link.dir._2)+
            ", " + (link.tarCenter._1 - 5.0 * link.dir._1) +" "+ (link.tarCenter._2 - 5.0 * link.dir._2)+
            ", " + (source.centerX - 4.0 * link.dir._1) +" "+ (source.centerY - 4.0 * link.dir._2)
      } else {
        "M" + link.centerX       +" "+ link.centerY +
            " C" + (link.centerX - .3 * link.length * link.dir._1) +" "+ (link.centerY - .3 * link.length * link.dir._2)+
            ", " + link.srcCenter._1    +" "+ link.srcCenter._2 +
            ", " + source.centerX             +" "+ source.centerY
      }
    }
    
    override def toString = source + "::" + link
  }

  
}

trait GraphView {
  val nodes = js.Array[GraphView.GraphNode]()
  val links = js.Array[GraphView.NodeLink]()
}
