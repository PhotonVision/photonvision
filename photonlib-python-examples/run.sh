# To run any example, we want to use photonlib out of this repo
# Build the wheel first
pushd ../photon-lib/py
if [ -d build ]
  then rm -rdf build
fi
python3 setup.py bdist_wheel
popd

# Add the output directory to PYTHONPATH to make sure it gets picked up
export PHOTONLIBPY_ROOT=../photon-lib/py
export PYTHONPATH=$PHOTONLIBPY_ROOT

# If an example to run is not provided, run all examples
if [ $# -eq 0 ]
  then
    echo "No example provided, running all examples"
    for dir in */
    do
      echo "Running example in $dir"
      ./run.sh $dir
    done
    exit 0
fi

# Move to the right example folder
cd $1

# Run the example
robotpy sim
