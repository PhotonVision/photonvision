# Check if the first argument is provided
if [ $# -eq 0 ]
  then
    echo "Error: No example-to-run provided."
    exit 1
fi

# Move to the right example folder
cd $1

# Run the example
python3 robot.py sim
