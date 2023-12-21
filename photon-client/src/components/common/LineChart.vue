<script>
import { Scatter } from "vue-chartjs";
import chartJsPluginAnnotation from "chartjs-plugin-annotation";

export default {
  extends: Scatter,
  props: {
    chartData: {
      type: Object,
      default: null
    },
    xmin: Number,
    xmax: Number,
    ymin: Number,
    ymax: Number,
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
              fontSize: 12
            }
          },
          scales: {
            yAxes: [
              {
                ticks: {
                  fontColor: "white"
                }
              }
            ],
            xAxes: [
              {
                ticks: {
                  fontColor: "white"
                }
              }
            ]
          },
          plugins: {
            annotation: {
              annotations: {
                box1: {
                  type: "box",
                  xMin: 0,
                  xMax: 100,
                  yMin: 0,
                  yMax: 100,
                  backgroundColor: "rgba(255, 128, 100, 0.5)"
                }
              }
            }
          }
        };
      }
    }
  },
  computed: {
    chartOptions: {
      get() {
        const opts = this.options;

        opts.scales.xAxes.forEach((it) => (it.ticks.min = (this.xmin)));
        opts.scales.xAxes.forEach((it) => (it.ticks.max = (this.xmax)));
        opts.scales.yAxes.forEach((it) => (it.ticks.min = (this.ymin)));
        opts.scales.yAxes.forEach((it) => (it.ticks.max = (this.ymax)));

        return opts;
      }
    }
  },
  mounted() {
    this.addPlugin(chartJsPluginAnnotation);
    // this.renderChart(this.chartData, this.chartOptions);
    this.renderChart(this.chartData, { 
      ...this.chartOptions, 
      annotation: Object.assign({}, this.chartOptions.annotation)
    })
  },
  // watch: {
  //   chartData() {
  //     this.renderChart(this.chartData, this.chartOptions)
  //   }
  // }
  methods: {
    update() {
      // this.renderChart(this.chartData, this.chartOptions);
      this.renderChart(this.chartData, { 
        ...this.chartOptions, 
        annotation: Object.assign({}, this.chartOptions.annotation)
      })
    }
  }
};
</script>
