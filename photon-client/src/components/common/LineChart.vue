<script>
import { Scatter } from "vue-chartjs";

export default {
  extends: Scatter,
  props: {
    chartData: {
      type: Object,
      default: null,
    },
    min: String,
    max: String,
    options: {
      animation: false,
      spanGaps: true,
      type: Object,
      default: () => {
        return {
          responsive: true,
          maintainAspectRatio: false,
          animation: { duration: 0 },
          legend: {
            labels: {
              fontColor: "white",
              fontSize: 12,
            },
          },
          scales: {
            yAxes: [
              //     {
              //   ticks: {
              //     fontColor: "white",
              //     fontSize: 12,
              //   }
              // }
            ],
            xAxes: [
              //     {
              //   type: "time",
              //   time: {
              //     unit: 'second',
              //     unitStepSize: 10,
              //   },
              //   ticks: {
              //     fontColor: "white",
              //     fontSize: 12,
              //   }
              // }
            ],
          },
        };
      },
    },
  },
  computed: {
    chartOptions: {
      get() {
        const opts = this.options;

        if (this.min) {
          opts.scales.yAxes.forEach(
            (it) => (it.ticks.suggestedMin = parseFloat(this.min))
          );
        }
        if (this.max) {
          opts.scales.yAxes.forEach(
            (it) => (it.ticks.suggestedMax = parseFloat(this.max))
          );
        }

        return opts;
      },
    },
  },
  mounted() {
    this.renderChart(this.chartData, this.chartOptions);
  },
  // watch: {
  //   chartData() {
  //     this.renderChart(this.chartData, this.chartOptions)
  //   }
  // }
  methods: {
    update() {
      this.renderChart(this.chartData, this.chartOptions);
    },
  },
};
</script>
