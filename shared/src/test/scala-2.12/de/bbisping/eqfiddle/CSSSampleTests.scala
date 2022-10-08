package de.bbisping.eqfiddle

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.BeMatcher
import org.scalatest.matchers.should

import de.bbisping.eqfiddle.tool.model.NodeID
import de.bbisping.eqfiddle.util.Relation
import de.bbisping.eqfiddle.hml.ObservationClass
import de.bbisping.eqfiddle.ts.WeakTransitionSystem
import de.bbisping.eqfiddle.algo.AlgorithmLogging
import de.bbisping.eqfiddle.spectroscopy.AbstractSpectroscopy
import de.bbisping.eqfiddle.spectroscopy.PositionalSpectroscopy
import de.bbisping.eqfiddle.hml.Spectrum
import de.bbisping.eqfiddle.hml.HennessyMilnerLogic
import de.bbisping.eqfiddle.spectroscopy.SpectroscopyInterface

trait CSSSampleTests[OC <: ObservationClass, CF <: HennessyMilnerLogic.Formula[String]] extends AnyFunSpec with should.Matchers  {

  AlgorithmLogging.debugLogActive = false

  def spectrum: Spectrum[OC]

  private def toSpectrumClassSet(names: Iterable[String]) = (for {
    n <- names
    cl <- spectrum.getSpectrumClass.get(n)
  } yield cl).toSet

  def runTest(
      sampleSystem: WeakTransitionSystem[NodeID,String,String],
      sampleNames: List[(String, String, List[String], List[String])],
      spectroscopyAlgo: (WeakTransitionSystem[NodeID,String,String]) => SpectroscopyInterface[NodeID,String,String,CF],
      title: String) = {
    val samples = sampleNames.map {
      case (n1, n2, preords, notPreords) =>
        (n1, n2, toSpectrumClassSet(preords), toSpectrumClassSet(notPreords))
    }
    describe("The Spectroscopy " + title) {
      forAll(samples) { case (n1s, n2s, preords, notPreords) =>
        describe("for " + n1s + " <= " + n2s) {
          val n1 = NodeID(n1s)
          val n2 = NodeID(n2s)

          val algo = spectroscopyAlgo(sampleSystem)

          val preordsStr = preords.map(_.name)
          val notPreordsStr = notPreords.map(_.name).intersect(algo.spectrum.notionNames)

          val result = algo.compute(List((n1, n2)))

          val foundDistinctions = result.foundDistinctions(n1, n2).map(d => d.name match { case "2failure" => "failure"; case "2trace" => "trace"; case n => n } ).toSet
          it ("should be distinguished by " + notPreordsStr.mkString(",")) {
            (notPreordsStr diff foundDistinctions) should be (empty)
          }

          val foundPreorders = result.foundPreorders(n1, n2).map(_.name).toSet
          it ("should exactly be preordered by " + preordsStr.mkString(",")) {
            if (!(preordsStr subsetOf algo.spectrum.notionNames)) cancel(s"$preordsStr do not apply for $title spectrum")
            (foundPreorders diff preordsStr) should be (empty)
            (preordsStr diff foundPreorders) should be (empty)
          }
        }
      }
    }
  }
}