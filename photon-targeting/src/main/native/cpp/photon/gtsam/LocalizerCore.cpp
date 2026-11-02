/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "photon/gtsam/LocalizerCore.h"

#include <iostream>
#include <utility>
#include <vector>

#include "gtsam/nonlinear/Expression.h"
#include "photon/estimation/TargetModel.h"

using namespace gtsam;
using symbol_shorthand::X;

namespace photon::pvgtsam {
LocalizerCore::LocalizerCore(FieldLayout fieldLayout)
    : fieldLayout(std::move(fieldLayout)) {
  ISAM2Params parameters;
  // parameters.relinearizeThreshold = 0.01;
  // parameters.relinearizeSkip = 1;
  // parameters.cacheLinearizedFactors = false;
  // parameters.enableDetailedResults = true;
  parameters.findUnusedFactorSlots = true;
  parameters.print();

  // TODO: make sure that timestamps in units of uS doesn't cause numerical
  // precision issues
  double lag = 5 * 1e6;
  // double lag = 2;
  smootherISAM2 = IncrementalFixedLagSmoother(lag, parameters);

  // // And make sure to call optimize first to get values
  // TODO i killed maybe needed, idk
  // Optimize();
}

void LocalizerCore::Reset(ResetData data) {
  // Anchor graph using initial pose. I subtract one to make sure that we dont
  // add this time to the estimate map twice
  data.timeUs -= 1;

  currStateIdx = X(data.timeUs);

  smootherISAM2 = IncrementalFixedLagSmoother(smootherISAM2.smootherLag(),
                                              smootherISAM2.params());

  graph.resize(0);
  currentEstimate.clear();
  newTimestamps.clear();
  factorsToRemove.clear();
  twistsFromPreviousKey.clear();

  graph.addPrior(currStateIdx, data.wTr, data.noise);
  currentEstimate.insert(currStateIdx, data.wTr);
  newTimestamps[currStateIdx] = data.timeUs;

  wTb_latest = data.wTr;
}

void LocalizerCore::AddOdometry(OdometryObservation odom) {
  Key newStateIdx = X(odom.timeUs);

  // Add an odometry pose delta from our last state to our new one
  graph.emplace_shared<BetweenFactor<Pose3>>(
      currStateIdx, newStateIdx, odom.poseDelta, odom.odometryNoise);

  // And get initial guess just by composing previous pose
  SetLatestWorldToBody(
      GetLatestWorldToBody().transformPoseFrom(odom.poseDelta));
  currentEstimate.insert(newStateIdx, GetLatestWorldToBody());

  newTimestamps[newStateIdx] = odom.timeUs;
  twistsFromPreviousKey[newStateIdx] = odom.poseDelta;
  latestOdomTime = odom.timeUs;

  currStateIdx = newStateIdx;
}

Key LocalizerCore::InsertIntoSmoother(Key lower, Key upper, Key newKey,
                                      double newTime) {
  const auto& isam = smootherISAM2.getISAM2();
  const VariableIndex& variableIndex = isam.getVariableIndex();
  const NonlinearFactorGraph& currentFactors = isam.getFactorsUnsafe();

  const auto factorsConnectedToUpper = variableIndex.find(upper);
  const auto factorsConnectedToLower = variableIndex.find(lower);

  if (factorsConnectedToUpper == variableIndex.end() ||
      factorsConnectedToLower == variableIndex.end()) {
    // unclear what to do lol
    return 0;
  }

  // we know there is exactly one factor connecting, so do sorting-at-home
  for (const FactorIndex& idxLower : factorsConnectedToLower->second) {
    for (const FactorIndex& idxUpper : factorsConnectedToUpper->second) {
      if (idxLower == idxUpper) {
        // found our connecting factor
        if (idxLower >= currentFactors.size() || !currentFactors[idxLower]) continue; // bail
        
        auto edge = std::dynamic_pointer_cast<BetweenFactor<Pose3>>(currentFactors[idxLower]);
        if (!edge || edge->key1() != lower || edge->key2() != upper) continue; // bail

        if (std::find(factorsToRemove.begin(), factorsToRemove.end(), idxLower) !=
        factorsToRemove.end()) {
          throw std::runtime_error("InsertIntoSmoother: edge already being split"); // bail
        }

        const auto& ts = smootherISAM2.timestamps();
        const double tLower = ts.at(lower);
        const double tUpper = ts.at(upper);
        const double t = (newTime - tLower) / (tUpper - tLower);

        const Pose3& delta = edge->measured();
        const SharedNoiseModel& noise = edge->noiseModel();

        // mark this factor for removal
        factorsToRemove.push_back(idxLower);

        const Vector6 totalTwist = Pose3::Logmap(delta);
        const Pose3 deltaLowerToMid = Pose3::Expmap(totalTwist * t);
        const Pose3 deltaMidToHigh = Pose3::Expmap(totalTwist * (1.0 - t));

        graph.emplace_shared<BetweenFactor<Pose3>>(
            lower, newKey, deltaLowerToMid, noise);
        graph.emplace_shared<BetweenFactor<Pose3>>(
            newKey, upper, deltaMidToHigh, noise);
        
        Pose3 currentWorldToLower =
            smootherISAM2.calculateEstimate<Pose3>(lower);
        currentEstimate.insert(
            newKey, currentWorldToLower.transformPoseFrom(deltaLowerToMid));
        newTimestamps[newKey] = newTime;
        twistsFromPreviousKey[newKey] = deltaLowerToMid;
        twistsFromPreviousKey[upper] = deltaMidToHigh;

        return newKey;
      }
    }
  }
  return 0; // bail
}

using KeyTimeConstIt = FixedLagSmoother::KeyTimestampMap::const_iterator;
static KeyTimeConstIt FindCloser(KeyTimeConstIt left, KeyTimeConstIt right,
                                 double time) {
  double deltaLeft = time - left->second;
  double deltaRight = right->second - time;
  if (deltaLeft < deltaRight) {
    return left;
  } else {
    return right;
  }
}

Key LocalizerCore::GetOrInsertKey(Key newKey, double time) {
  using KeyTimeMap = FixedLagSmoother::KeyTimestampMap;

  const KeyTimeMap& isamTimestamps = smootherISAM2.timestamps();
  const auto& isamEntryAfter = isamTimestamps.upper_bound(newKey);
  if (isamEntryAfter == isamTimestamps.begin()) {
    throw std::runtime_error("Timestamp is before even isam history");
  }

  // safe to do this, we checked we aren't at the start
  const auto& isamEntryBefore = std::prev(isamEntryAfter);

  if (isamEntryAfter != isamTimestamps.end() &&
      isamEntryBefore->second < time) {
    // must be fully within isam
    // return FindCloser(isamEntryBefore, isamEntryAfter, time)->first;
    // Fully within ISAM: split the odometry edge instead of snapping
    constexpr double kSnapUs = 2000;  // don't create near-zero-length edges
    if (time - isamEntryBefore->second < kSnapUs) return isamEntryBefore->first;
    if (isamEntryAfter->second - time < kSnapUs) return isamEntryAfter->first;

    return InsertIntoSmoother(isamEntryBefore->first,
                                    isamEntryAfter->first, newKey, time);
  }

  KeyTimeMap::iterator notAddedAfter = newTimestamps.upper_bound(newKey);

  if (notAddedAfter == newTimestamps.end()) {
    throw std::runtime_error(
        "Timestamp past ISAM history, but not in yet-to-be-added");
  }

  if (notAddedAfter == newTimestamps.begin() &&
      notAddedAfter != newTimestamps.end()) {
    // check in between maybe?
    if (isamEntryBefore->second < time && time < notAddedAfter->second) {
      return FindCloser(isamEntryBefore, notAddedAfter, time)->first;
    }
    throw std::runtime_error(
        "Timestamp is before not-added but not after isam history?");
  }

  KeyTimeMap::iterator notAddedBefore = std::prev(notAddedAfter);

  if (isamEntryAfter != isamTimestamps.end() &&
      isamEntryBefore->second < time) {
    // must be fully within isam
    return FindCloser(isamEntryBefore, isamEntryAfter, time)->first;
  } else if (notAddedAfter != newTimestamps.end() &&
             notAddedBefore->second < time) {
    // must be fully within not added
    return FindCloser(notAddedBefore, notAddedAfter, time)->first;
  } else {
    // already checked in between
    throw std::runtime_error("wtf");
  }

  // // Step 1: Check if the exact time is already in our graph
  // {
  //   // Check trivial case, about to be added to the smoother
  //   KeyTimeMap::iterator keyIter = newTimestamps.find(newKey);

  //   if(keyIter != newTimestamps.end()) {
  //     return keyIter->first;
  //   }
  // }
  // {
  //   const KeyTimeMap& timestamps = smootherISAM2.timestamps();

  //   // Check trivial case, time already in the smoother
  //   KeyTimeMap::const_iterator keyIter = timestamps.find(newKey);

  //   if(keyIter != timestamps.end()) {
  //     return keyIter->first;
  //   }
  // }

  // // Step 2: check if the time is fully contained within our graph
  // {
  //   const KeyTimeMap& timestamps = smootherISAM2.timestamps();
  //   // Iterator pointing to the first element greater than key, or end().
  //   // OR: first entry greater than time
  //   const auto& keyJustAfter = timestamps.upper_bound(newKey);

  //   // Make sure new time would be before history ends
  //   if (keyJustAfter != timestamps.end()) {
  //     // Case 1 -- we are inserting a factor before the smoother's history.
  //     Best we can do is bail if (keyJustAfter == timestamps.begin()) {
  //       throw std::runtime_error("Timestamp has been marginalized out!");
  //     }

  //     // Case 2 -- fully within smoother history, So we have (in increasing
  //     time order)
  //     // keyJustBefore < now < keyJustAfter

  //     // safe to do this now
  //     const auto& keyJustBefore = std::prev(keyJustAfter);

  //     return InsertIntoSmoother(keyJustBefore->first, keyJustAfter->first,
  //     newKey, time);

  //     // // Delete the odometry factor between these two kys
  //     // // HACK: is there a less bad nested iteration way to do this?
  //     // const NonlinearFactorGraph& factors = smootherISAM2.getFactors();
  //     // for (const auto& factor : factors) {
  //     //   // Find a factor that has the before/after keys as its keys
  //     //   if (factor.find(keyJustBefore.first) != factor.end() &&
  //     factor.find(keyJustAfter.first) != factor.end() && factor.size() == 2)
  //     {
  //     //     // This is probably the odometry factor???

  //     //   }
  //     // }
  //   }

  //   // else: timestmap is after smoother history ends, hopefully the next
  //   step can deal with this
  // }

  // // Step 3: check if the timestamp is in between end of smoother and start
  // of queued factors we haven't added yet
  // {
  //   // We know time is past the end of our smoother buffer, but not sure
  //   where in our timestamp list it will fall

  //   const KeyTimeMap& timestamps = smootherISAM2.timestamps();
  //   // Iterator pointing to the first element greater than key, or end().
  //   // OR: first entry greater than time
  //   const auto& keyJustBefore = std::prev(timestamps.end());

  //   // Since newTimestamps is ordered by key, and keys increase with
  //   timestamp FixedLagSmoother::KeyTimestampMap::const_iterator keyJustAfter
  //   = newTimestamps.end();

  //   if (keyJustAfter->second > time) {
  //     // yay! Same hack wrt snap to closest factor still applies

  //     double dtToBefore = time - keyJustBefore->first;
  //     double dtToAfter = keyJustAfter->second - time;
  //     if (dtToBefore < dtToAfter) {
  //       return keyJustBefore->second;
  //     } else {
  //       return keyJustAfter->first;
  //     }
  //   } else {
  //     // else: time must be fully in our keys we're about to add
  //   }

  // }

  // // Step 4: timestamp either fully within factors we haven't added yet, or
  // is past the tip/too far in the future
  // {
  //   // TODO: somehow reverse this map since I need to look-up by timestmap,
  //   not by key
  //   // TODO HACK UGH
  //   FixedLagSmoother::KeyTimestampMap::iterator keyJustAfter =
  //   newTimestamps.upper_bound(newKey);

  //   // Make sure new time would be before history ends
  //   if (keyJustAfter != newTimestamps.end()) {
  //     // Case 1 -- we are inserting a factor between smoother and new factors
  //     (which should be covered by the above). Ignore?

  //     // Case 2 -- fully within new factor history

  //     // safe to do this now
  //     const auto& keyJustBefore = std::prev(keyJustAfter);

  //     // same justification as above
  //     double dtToBefore = time - keyJustBefore->second;
  //     double dtToAfter = keyJustAfter->second - time;
  //     if (dtToBefore < dtToAfter) {
  //       return keyJustBefore->first;
  //     } else {
  //       return keyJustAfter->first;
  //     }

  //     // TODO: implement this instead
  //     // Since we haven't added to the optimizer yet, we are good to be smart
  //     and reorder factors
  //     /*
  //     ALGORITHM:
  //     - Find the twist between a and b
  //     - delete the factor between a and b
  //     - insert a new state, c, between a and b temporally
  //     - connect a -- c -- b using twists
  //       - convert betweenfactor pose delta to a twist
  //       - lerp twist as factor of the dt between a/c/b
  //       - reconnect a-c and c-b using between factors
  //       - return c
  //     */
  //   }

  //   // else: timestmap is after smoother history AND factor to add history
  //   // drop it on the floor or smth
  //   throw std::runtime_error("Sample is too new to add??");
  // }
}

void LocalizerCore::AddTagObservation(CameraVisionObservation obs) {
  const auto& isamTimestamps = smootherISAM2.timestamps();
  if (obs.timeUs < isamTimestamps.begin()->second) {
    std::cerr << "Timestamp is before even isam history - skipping"
              << std::endl;
    return;
  }

  auto worldPcorners_opt = fieldLayout.WorldToCorners(obs.tagID);
  if (!worldPcorners_opt) {
    // todo return bad thing
    // fmt::println("Could not find tag {} in our map!", tagID); fmt doesn't
    // work anymore for some reason? I blame wpilib
    return;
  }
  auto worldPcorners = worldPcorners_opt.value();

  Key newKey = X(obs.timeUs);

  // Find where we should attach our new factors to
  Key stateAtTime = GetOrInsertKey(newKey, obs.timeUs);

  for (size_t i = 0; i < 4; i++) {
    // corner in image space
    Point2 measurement = obs.corners[i];

    // current world->body pose
    const Pose3_ worldTbody_fac(stateAtTime);
    const auto prediction = PredictLandmarkImageLocation(
        worldTbody_fac, obs.robotTcamera, obs.cameraCal, worldPcorners[i]);

    graph.addExpressionFactor(prediction, measurement, obs.cameraNoise);
  }
}

void LocalizerCore::Optimize() {
  // fmt::println("Adding {} factors!", graph.size());
  // graph.print("New factors: ");
  // currentEstimate.print("New estimates: ");

  smootherISAM2.update(graph, currentEstimate, newTimestamps, factorsToRemove);

  // reset the graph; isam wants to be fed factors to be -added-
  graph.resize(0);
  currentEstimate.clear();
  newTimestamps.clear();
  factorsToRemove.clear();

  // And grab the estimate of only the latest pose (maximize laziness)
  // Cache for use with FK prediction when adding odom factors
  SetLatestWorldToBody(smootherISAM2.calculateEstimate<Pose3>(currStateIdx));
}

gtsam::Pose3 LocalizerCore::GetLatestWorldToBody() const {
  std::lock_guard<std::mutex> lock(wTr_mtx);
  return wTb_latest;
}

Matrix LocalizerCore::GetLatestMarginals() const {
  return smootherISAM2.marginalCovariance(GetCurrStateIdx());
}

Vector6 LocalizerCore::GetPoseComponentStdDevs() const {
  Matrix marginals = GetLatestMarginals();
  return marginals.diagonal().cwiseSqrt();
}

std::vector<wpi::math::Pose3d> LocalizerCore::GetPoseHistory() const {
  // Plot all history, so grab the whole estimate
  Values result = smootherISAM2.calculateEstimate();

  // 5 seconds of history
  auto start = currStateIdx - (5 * 1e6);

  std::vector<wpi::math::Pose3d> ret;
  ret.reserve(1000);

  for (const Values::ConstKeyValuePair& estPair : result) {
    if (estPair.key < start) continue;

    Pose3 est = estPair.value.cast<Pose3>();

    // auto rot = est.rotation().toQuaternion();
    // vector<double> poseEst{est.x(), est.y(), est.z(), rot.w(),
    //                             rot.x(), rot.y(), rot.z()};

    ret.emplace_back(wpi::math::Translation3d{wpi::units::meter_t{est.x()},
                                              wpi::units::meter_t{est.y()},
                                              wpi::units::meter_t{est.z()}},
                     wpi::math::Rotation3d{est.rotation().matrix()});
  }

  return ret;
}

void LocalizerCore::SetLatestWorldToBody(gtsam::Pose3 wTb) {
  std::lock_guard<std::mutex> lock(wTr_mtx);
  wTb_latest = wTb;
}
}  // namespace photon::pvgtsam
