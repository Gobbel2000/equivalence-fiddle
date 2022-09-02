package de.bbisping.eqfiddle

import de.bbisping.eqfiddle.spectroscopy.PositionalSpectroscopy
import de.bbisping.eqfiddle.spectroscopy.EdgeSpectroscopy
import de.bbisping.eqfiddle.hml.ObservationClassStrong
import de.bbisping.eqfiddle.hml.ObservationClassStrong.StronglyClassifiedFormula


class EdgeCaseTests extends CSSSampleTests[ObservationClassStrong, StronglyClassifiedFormula[String]] {
  override val spectrum = ObservationClassStrong.LTBTS
  
  val reviewSystem = TestSamples.samples.find(_._1 == "review-counterexamples").get._2

  val sampleNames: List[(String, String, List[String], List[String])] = List(
    ("L1", "R1", List("readiness", "simulation"), List("failure-trace", "impossible-future")),
    ("L2", "R2", List("readiness", "simulation"), List("failure-trace", "impossible-future"))
  )

  runTest(reviewSystem, sampleNames, new PositionalSpectroscopy(_, _), "(positional)")
  runTest(reviewSystem, sampleNames, new EdgeSpectroscopy(_, _), "(edge-labeled)")
}