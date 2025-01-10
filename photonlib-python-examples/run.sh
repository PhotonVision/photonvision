# Check if the first argument is provided
if [ $# -eq 0 ]
  then
    echo "Error: No example-to-run provided."
    exit 1
fi

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

# Move to the right example folder
cd $1

# Run the example
robotpy sim
