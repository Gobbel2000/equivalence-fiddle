package de.bbisping.coupledsim.algo.contra

import de.bbisping.coupledsim.util.Relation
import de.bbisping.coupledsim.ts.WeakTransitionSystem
import de.bbisping.coupledsim.util.FixedPoint
import scala.collection.mutable.Queue
import de.bbisping.coupledsim.algo.AlgorithmLogging
import de.bbisping.coupledsim.game.WinningRegionComputation
import de.bbisping.coupledsim.game.GameDiscovery
import de.bbisping.coupledsim.game.SimpleGame


class GameContrasimilarityExponential[S, A, L] (
    val ts: WeakTransitionSystem[S, A, L])
  extends AlgorithmLogging[S, A, L] {

  case class ContraAttackerNode(p: S, qq: Set[S]) extends SimpleGame.AttackerNode
  case class ContraDefenderCouplingNode(p: S, qq: Set[S]) extends SimpleGame.DefenderNode

  class ContrasimulationExponentialGame
    extends SimpleGame with GameDiscovery with WinningRegionComputation {

    override def initialNodes: Iterable[SimpleGame.GameNode] = for {
      s1 <- ts.nodes
      s2 <- ts.nodes
    } yield ContraAttackerNode(s1, Set(s2))

    def successors(gn: GameNode): Iterable[GameNode] = gn match {
      case ContraAttackerNode(p0, qq0) =>
        val dn = for {
          (a,pp1) <- ts.post(p0)
          p1 <- pp1
        } yield if (ts.silentActions(a)) {
          ContraAttackerNode(p1, qq0)
        } else {
          ContraAttackerNode(p1, qq0.flatMap(ts.weakPostDelay(_, a)))
        }
        dn ++ List(ContraDefenderCouplingNode(p0, qq0))
      case ContraDefenderCouplingNode(p0, qq0) =>
        for {
          q0 <- qq0
          q1 <- ts.silentReachable(q0)
        } yield ContraAttackerNode(q1, Set(p0))
    }
  }



  def compute() = {

    val csGame = new ContrasimulationExponentialGame()

    println("cs plain game size: " + csGame.discovered.size)

    val attackerWin = csGame.computeWinningRegion()

    // the coupled simulation is exactly the attacker nodes not won by the attacker
    val simNodes = for {
      gn <- csGame.discovered
      if gn.isInstanceOf[ContraAttackerNode] && !attackerWin(gn)
      ContraAttackerNode(p, qq) = gn
      if qq.size == 1
      q <- qq
    } yield (p, q)

    new Relation[S](simNodes.toSet)
  }
}