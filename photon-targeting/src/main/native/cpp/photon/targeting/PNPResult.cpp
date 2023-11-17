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

#include "photon/targeting/PNPResult.h"

using namespace photon;

PNPResult::PNPResult(frc::Transform3d best, double bestReprojErr,
                             frc::Transform3d alt, double altReprojErr,
                             double ambiguity) : best(best), bestReprojErr(bestReprojErr), alt(alt), altReprojErr(altReprojErr), ambiguity(ambiguity) {
                                this->isPresent = true;
                             }



bool PNPResult::operator==(const PNPResult& other) const {
  return other.isPresent == isPresent 
            && other.best == best 
            && other.altReprojErr == bestReprojErr 
            && other.alt == alt 
            && other.altReprojErr == altReprojErr 
            && other.ambiguity == ambiguity;
}
