package de.bbisping.eqfiddle.spectroscopy

import de.bbisping.eqfiddle.util.Relation
import de.bbisping.eqfiddle.util.LabeledRelation
import de.bbisping.eqfiddle.util.Coloring

import de.bbisping.eqfiddle.hml.HennessyMilnerLogic
import de.bbisping.eqfiddle.hml.ObservationClass
import de.bbisping.eqfiddle.hml.Spectrum

trait SpectroscopyInterface[S, A, L, CF <: HennessyMilnerLogic.Formula[A]] {

  def spectrum: Spectrum[ObservationClass]

  def compute(comparedPairs: Iterable[(S,S)]): SpectroscopyInterface.SpectroscopyResult[S, A, ObservationClass, CF]
}

object SpectroscopyInterface {

  case class SpectroscopyResultItem[S, A, +OC <: ObservationClass, +OF <: HennessyMilnerLogic.Formula[A]](
    left: S,
    right: S,
    distinctions: List[(OF, OC, List[Spectrum.EquivalenceNotion[OC]])],
    preorderings: List[Spectrum.EquivalenceNotion[OC]]
  ) {
    def serialize(listConstructor: (Iterable[Any] => Any), mapConstructor: (Map[String, Any] => Any)) = {
      val dists = for {
        (f, price, eqs) <- distinctions
      } yield mapConstructor(Map(
        ("formula", f.toString()),
        ("price", listConstructor(price.toTuple.productIterator.toIterable)),
        ("inequivalences", listConstructor(eqs.map(_.name))))
      )
      mapConstructor(Map(
        ("left", left.toString()),
        ("right", right.toString()),
        ("distinctions", listConstructor(dists)),
        ("preorderings", listConstructor(preorderings.map(_.name)))
      ))
    }
  }

  case class SpectroscopyResult[S, A, +OC <: ObservationClass, +OF <:HennessyMilnerLogic.Formula[A]](
      val relationItems: List[SpectroscopyResultItem[S, A, OC, OF]], val spectrum: Spectrum[OC]) {

    def resultFor(p: S, q: S) = {
      for {
        res <- relationItems
        if res.left == p && res.right == q
      } yield res
    }

    def toDistinctionRelation() = {
      val relTuples = for {
        SpectroscopyResultItem(l, r, dists, preords) <- relationItems
        dis <- dists
        inEqs = dis._3.map(_.name).mkString(" (", ",", ")")
      } yield (l, dis._1.toString() + inEqs, r)
      new LabeledRelation(relTuples.toSet)
    }

    def toPreorderingRelation() = {
      val relTuples = for {
        SpectroscopyResultItem(l, r, dists, preords) <- relationItems
        pre <- preords
      } yield (l, pre.name.toString(), r)
      new LabeledRelation(relTuples.toSet)
    }

    def toEquivalencesRelation() = {
      val undirectedTuples = {
        for {
          SpectroscopyResultItem(l, r, _, _) <- relationItems
          if l != r
        } yield Set(l, r)
      }.toSet
      val relTuples = for {
        pair <- undirectedTuples
        orderedPair = pair.toList
        p = orderedPair(0)
        q = orderedPair(1)
        eq <- findEqs(p, q)
      } yield (p, eq.name + " eq", q)
      new LabeledRelation(relTuples.toSet)
    }

    def toDistancesRelation[OCC >: OC](): LabeledRelation[S,Set[OCC]] = {
      val undirectedResults = relationItems.groupBy(r => Set(r.left, r.right))
      val undirectedDistinctions = for {
        (pq, results) <- undirectedResults
        if pq.size == 2
      } yield (pq, results.flatMap(_.distinctions.map(_._2)).toSet)
      val relTuples = for {
        (pq, dist) <- undirectedDistinctions
        orderedPair = pq.toList
        p = orderedPair(0)
        q = orderedPair(1)
      } yield (p, dist.map(_.asInstanceOf[OCC]), q)
      new LabeledRelation(relTuples.toSet)
    }

    def toQuotients(eqs: Iterable[Spectrum.EquivalenceNotion[ObservationClass]], rep: (S, S) => S, initialApproximation: Iterable[(S,S)]): Iterable[Coloring[S]] = {
      val distances = toDistancesRelation().symmetricReflexiveClosure(initialApproximation.map(_._1), Set())
      for {
        eq <- eqs
      } yield {
        distances.toQuotientColoring(
          dist => !dist.exists(d => d <= eq.obsClass),
          rep
        )
      }
    }

    def foundPreorders(p: S, q: S): List[Spectrum.EquivalenceNotion[OC]] = {
      for {
        res <- resultFor(p, q)
        preord <- res.preorderings
      } yield preord
    }

    def foundDistinctions(p: S, q: S): List[Spectrum.EquivalenceNotion[OC]] = {
      for {
        res <- resultFor(p, q)
        dists <- res.distinctions
        dis <- dists._3
      } yield dis
    }

    def findEqs(p: S, q: S): List[Spectrum.EquivalenceNotion[OC]] = {
      val distinctionClasses = for {
        res <- relationItems
        if (res.left == p && res.right == q) || (res.left == q && res.right == p)
        dists <- res.distinctions
        disClass <- dists._3
      } yield disClass
      spectrum.getStrongestPreorderClass(distinctionClasses)
    }

    def printStats(): String = {
      s"Relation Items: ${relationItems.size}"
    }

  }

}