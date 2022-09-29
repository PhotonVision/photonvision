/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#pragma once

#include <Eigen/Core>

namespace frc {

template <int Size>
using Vectord = Eigen::Vector<double, Size>;

template <int Rows, int Cols,
          int Options = Eigen::AutoAlign |
                        ((Rows == 1 && Cols != 1) ? Eigen::RowMajor
                         : (Cols == 1 && Rows != 1)
                             ? Eigen::ColMajor
                             : EIGEN_DEFAULT_MATRIX_STORAGE_ORDER_OPTION),
          int MaxRows = Rows, int MaxCols = Cols>
using Matrixd = Eigen::Matrix<double, Rows, Cols, Options, MaxRows, MaxCols>;

}  // namespace frc
