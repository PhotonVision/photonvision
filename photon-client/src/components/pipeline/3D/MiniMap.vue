<template>
  <div>
    <v-row>
      <v-col
        align="center"
        cols="12"
      >
        <span class="white--text">Target Location</span>
        <canvas
          id="canvasId"
          class="mt-2"
          width="800"
          height="800"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>
    import theme from "../../../theme";

    export default {
        name: "MiniMap",
        props: {
          // eslint-disable-next-line vue/require-default-prop
          targets: Array,
          // eslint-disable-next-line vue/require-default-prop
          horizontalFOV: Number
        },
        data() {
            return {
                ctx: undefined,
                canvas: undefined,
                x: 0,
                y: 0,
                targetWidth: 40,
                targetHeight: 6
            }
        },
        computed: {
            hLen: {
                get() {
                    return Math.tan(this.horizontalFOV / 2 * Math.PI / 180) * 150;
                }
            }
        },
        watch: {
            targets: {
                deep: true,
                handler() {
                    this.draw();
                }
            },
            horizontalFOV() {
                this.draw();
            }
        },
        mounted: function () {
            const canvas = document.getElementById("canvasId"); // getting the canvas element
            const ctx = canvas.getContext("2d"); // getting the canvas context
            this.canvas = canvas; // setting the canvas as a vue variable
            this.ctx = ctx; // setting the canvas context as a vue variable
            this.grad = this.ctx.createLinearGradient(400, 800, 400, 600);
            this.grad.addColorStop(0, "rgb(119,119,119)");
            this.grad.addColorStop(0.05, "rgba(14,92,22,0.96)");
            this.grad.addColorStop(0.8, 'rgba(43,43,43,0.48)');

            // setting canvas context values for drawing


            this.ctx.font = "26px Arial";
            this.ctx.strokeStyle = "whitesmoke";
            this.ctx.lineWidth = 2;

            this.$nextTick(function () {
                this.drawPlayer();
            });
        },
        methods: {
            draw() {
                this.clearBoard();
                this.drawPlayer();
                for (let index in this.targets) {
                    this.drawTarget(index, this.targets[index].pose);
                }
            },
            drawTarget(index, target) {
                // first save the untranslated/unrotated context
                let x = 800 - (160 * target.x); // getting meters as pixels
                let y = 400 - (160 * target.y);
                this.ctx.save();
                this.ctx.beginPath();
                // move the rotation point to the center of the rect
                this.ctx.translate(y + this.targetWidth / 2, x + this.targetHeight / 2); // wpi lib makes x forward and back and y left to right
                // rotate the rect
                this.ctx.rotate(target.rot * -1 * Math.PI / 180.0);

                // draw the rect on the transformed context
                // Note: after transforming [0,0] is visually [x,y]
                //       so the rect needs to be offset accordingly when drawn
                this.ctx.rect(-this.targetWidth / 2, -this.targetHeight / 2, this.targetWidth, this.targetHeight);

                this.ctx.fillStyle = theme.accent;
                this.ctx.fill();

                // restore the context to its untranslated/unrotated state
                this.ctx.restore();
                this.ctx.fillStyle = "whitesmoke";
                this.ctx.beginPath();
                this.ctx.arc(y + this.targetWidth / 2, x + this.targetHeight / 2, 3, 0, 2 * Math.PI, true);
                this.ctx.fill();
                this.ctx.fillText(index, y - 30, x - 5);

            },
            drawPlayer() {
                this.ctx.beginPath();
                this.ctx.moveTo(400, 820);
                this.ctx.lineTo(400 + this.hLen, 650);
                this.ctx.lineTo(400 - this.hLen, 650);
                this.ctx.closePath();
                this.ctx.fillStyle = this.grad;
                this.ctx.fill();
                this.ctx.beginPath();
                this.ctx.moveTo(400, 820);
                this.ctx.lineTo(400 + this.hLen, 650);
                this.ctx.stroke();
                this.ctx.moveTo(400, 820);
                this.ctx.lineTo(400 - this.hLen, 650);
                this.ctx.stroke();

            },
            clearBoard() {
                this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height); // clearing the canvas
            }
        }
    }
</script>

<style scoped>
    #canvasId {
        width: 400px;
        height: 400px;
        background-color: #232C37;
        border-radius: 5px;
        border: 2px solid grey;
        box-shadow: 0 0 5px 1px;
    }

    th {
        width: 80px;
        text-align: center;
    }
</style>