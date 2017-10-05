package sg.beeline

import org.scalatest._
import sg.beeline.problem._
import sg.beeline.ruinrecreate._
import sg.beeline.util.Util
import scala.util.Random
import Util.toSVY

class RuinSpec extends FlatSpec with Matchers {
  val TIME = 8 * 3600 * 1000

  // Randomly generate 100 bus stops
  def randomLatLng = {(Math.random() * 0.15 - 0.075 + 103.8, Math.random() * 0.15 - 0.075 + 1.38)}
  val latlngs = for (i <- 0 until 100) yield randomLatLng
  val busStops = latlngs.zipWithIndex.map({
    case (ll, i) => new BusStop(ll, i, s"BS ${i}", s"R ${i}", i)
    case _ => throw new Error()
  })

  val perturb : ((Double,Double)) => (Double,Double) =
    {case (x: Double,y :Double) => (x + Math.random() * 0.0001 - 0.00005, y + Math.random() * 0.0001 - 0.00005) }

  val starts = Random.shuffle(latlngs).map(ll => toSVY(perturb(ll)))
  val ends = Random.shuffle(latlngs).map(ll => toSVY(perturb(ll)))

  val requests = (starts zip ends).map({case (s,e) => new Suggestion(s,e,TIME)})

  val problem = new BasicRoutingProblem(busStops, requests)

  val (routes, validRequests, _) = problem.initialize

  val (preserved, ruined) = Ruin.ruin(problem, routes.toList, validRequests)

  "BasicRoutingProblem" should "preserve requests on initialize" in {
    validRequests.toSet should be (problem.requests.toSet)
  }

  "Ruin" should "preserve all requests" in {
    val preservedRequestSet = preserved.flatMap(r => r.activities.flatMap({
      case Pickup(req, loc) => Some(req)
      case Dropoff(req, loc) => Some(req)
      case _ => None
    })).toSet
    val ruinedRequestSet = ruined

    println(preservedRequestSet.size)
    println(ruinedRequestSet.size)

    (preservedRequestSet & ruinedRequestSet.toSet).size should be (0)



//    (preservedRequestSet ++ ruinedRequestSet) should be (r2.toSet)
    val finalSet = preservedRequestSet ++ ruinedRequestSet

    (finalSet -- validRequests.toSet) should be (Set())
    (validRequests.toSet -- finalSet) should be (Set())
  }

  "Recreate" should "preserve all requests" in {
    val (recreated, rejected) = Recreate.recreate(problem, preserved, ruined)
    val recreatedRequestSet = recreated.flatMap(r => r.activities.flatMap({
      case Pickup(req, loc) => Some(req)
      case Dropoff(req, loc) => Some(req)
      case _ => None
    })).toSet

    (recreatedRequestSet ++ rejected) should be (validRequests.toSet)
  }

  "LowestRegretRecreate" should "preserve all requests" in {
    val (recreated, rejected) = LowestRegretRecreate.recreate(problem, preserved, ruined)
    val recreatedRequestSet = recreated.flatMap(r => r.activities.flatMap({
      case Pickup(req, loc) => Some(req)
      case Dropoff(req, loc) => Some(req)
      case _ => None
    })).toSet

    (recreatedRequestSet ++ rejected) should be (validRequests.toSet)
  }

  "BeelineRecreate" should "preserve all requests" in {
    val beelineRecreate = new BeelineRecreate(problem, problem.requests)
    val (recreated, rejected) = beelineRecreate.recreate(problem, preserved, ruined)

    val recreatedRequestSet = recreated.flatMap(r => r.activities.flatMap({
      case Pickup(req, loc) => Some(req)
      case Dropoff(req, loc) => Some(req)
      case _ => None
    })).toSet

    (recreatedRequestSet ++ rejected) should be (validRequests.toSet)
  }
}