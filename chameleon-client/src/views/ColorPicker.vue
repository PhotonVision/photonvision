<template>
    <div>
        <div>
            <img id="sourceImg" ref="sourceImg" src="http://localhost:1181/stream.mjpg" style="display:none;image-rendering: pixelated;" crossorigin="Anonymous">
            <canvas id="imageCanvas" ref="imageCanvas" style="border:1px solid #000000;"/>
            <canvas id="zoomCanvas" ref="zoomCanvas" style="border:1px solid #000000;"/>
            <v-btn v-on:click="fetchAndDraw" tile color="#4baf62">Fetch New Image</v-btn>
            <v-btn v-on:click="removeSelected" tile color="#4baf62">Remove Selected</v-btn>
            <v-btn v-on:click="createRanges" tile color="#4baf62">Create Ranges</v-btn>
        </div>
        <div>
             <v-card 
        class="mx-auto"
        max-width="400"
        dark
      >
        <v-list
        >
        Colors choosen:
          <v-list-item-group
            v-model="model"
            :multiple="true"
            color="indigo"
          >
            <v-list-item
              v-for="i in colors.length-1"
              :key="i"
              :style="{'background': '#'+fullColorHex(colors[i])}"
            >  
              <v-list-item-content >
                <v-list-item-title >A</v-list-item-title>
              </v-list-item-content>             
            </v-list-item>
          </v-list-item-group>
        </v-list>
      </v-card>
        </div>
    </div>
</template>
<script>
export default {
    name: 'ColorPicker',
    data: function(){
        return{
        img:null,//the img tag, contains the mjpg stream, used to fetch the image
        ctxI: null,//the imageCanvas's context
        ctxZ: null,//The zoomCanvas's context
        model: [],
        colors: [[]],
    };
}
    ,
    components: {}
    ,
    methods: {
        createRanges: function()
        {
            let hsv = [];
            for(let i =1;i<this.colors.length;i++)//starting in 1 becase colors[0] is observer value or something
                hsv.push(this.RGBtoHSV(this.colors[i]));
            if(hsv.length>0)
            console.log(this.createRange(hsv));
        },

        fetchAndDraw: function() {
            console.log("fetching new image");
            // make the pixel at index red  
            //TODO set canvas width to a prestage from screen width and height
            this.ctxI.canvas.width=this.img.width;
            this.ctxI.canvas.height=this.img.height;            
            this.ctxI.drawImage(this.img, 0, 0, this.img.width, this.img.height, 0, 0,this.img.width, this.img.height);
            this.ctxZ.canvas.width=this.img.width;
            this.ctxZ.canvas.height=this.img.height;            
            this.ctxZ.imageSmoothingEnabled = false;//makes the zoomed in canvas pixelated and not blurry
        },
        //utilitie functions
        getMousePos: function(canvas, evt) {
            var rect = canvas.getBoundingClientRect();
            return {
                x: evt.clientX - rect.left,
                y: evt.clientY - rect.top
            };
        },
        
        removeSelected: function()
        {                    
            for (var i = 0;i<this.model.length;i++) {
                this.colors.splice(this.model[i]+1,1);//cuts one element off the array, this.model[i]+1 is because colors[0] is some other value observer or something
            }
            this.model=[];//clears out the to remove selection
        },

        //numbers is an array of 3 rgb values, returns array for 3 hsv values
        RGBtoHSV: function(numbers) {
            let r = numbers[0],
                g = numbers[1],
                b = numbers[2];
            r = r / 255;
            g = g / 255;
            b = b / 255;
            let minRGB = Math.min(r, Math.min(g, b));
            let maxRGB = Math.max(r, Math.max(g, b));
            let d = (r == minRGB) ? g - b : ((b == minRGB) ? r - g : b - r);
            let h = (r == minRGB) ? 3 : ((b == minRGB) ? 1 : 5);
            let H = 30 * (h - d / (maxRGB - minRGB));
            let S = 255 * (maxRGB - minRGB) / maxRGB;
            let V = 255 * maxRGB;
            if(isNaN(H))
                H=0;
            if(isNaN(S))
                S=0;
            if(isNaN(V))
                V=0;
            return [Math.round(H), Math.round(S), Math.round(V)];
        },

        rgbToHex: function(rgb) {
            var hex = Number(rgb).toString(16);
            if (hex.length < 2) {
                hex = "0" + hex;
            }
            return hex;
        },

        fullColorHex: function(color) {            
            var red = this.rgbToHex(color[0]);
            var green = this.rgbToHex(color[1]);
            var blue = this.rgbToHex(color[2]);
            return red + green + blue;
        },

        createRange: function(HSVColors)
        {
            let range = [];
            for (var i = 0; i < 3; i++) {
                range[i]=HSVColors[0][i];
                range[i+3]=HSVColors[0][i];
                for (var j = HSVColors.length - 1; j >= 0; j--) {
                    range[i]=Math.min(HSVColors[j][i],range[i]);
                    range[i+3]=Math.max(HSVColors[j][i],range[i+3]);
                }
                //Expanding the range by 10
                range[i]=Math.max(0,range[i]-10);               
                range[i+3]=Math.min(255,range[i+3]+10);
            }
            range[3]=Math.min(180,range[3]);//h is up to 180
            return range;
        },

        arraysEqual: function(a,b) {
        for(let i=0;i<a.length;i++)
        {
            if(!(a[i]==b[i]))
                return false;
        }
        return true;
    },
    }
    ,
    mounted: function() {
        //Init function, runs once when the page loads
        this.ctxI=this.$refs.imageCanvas.getContext("2d");
        this.ctxZ=this.$refs.zoomCanvas.getContext("2d");
        this.img=this.$refs.sourceImg;

        if (this.img.complete) {//calls fetchAndDraw when the image loads
          this.fetchAndDraw();
        } else {
          this.img.addEventListener('load', this.fetchAndDraw);
        }

        let a = this;   //inside the event lambda there is no access to 'this',use a instead
        //probably not very smart to do it this way

        //Listener of mouse movment over imageCanvas
        this.ctxI.canvas.addEventListener('mousemove', function(evt) {
            let mousePos = a.getMousePos(a.ctxI.canvas, evt);//Gets mouse pos
            a.ctxZ.clearRect(0, 0, a.ctxZ.canvas.width, a.ctxZ.canvas.height);//clears the canvas
            var scale = 10;//How much to zoom in
            a.ctxZ.drawImage(a.img, mousePos.x - a.img.width / (2 * scale), mousePos.y - a.img.height / (2 * scale), a.img.width / scale, a.img.height / scale, 0, 0, a.ctxZ.canvas.width, a.ctxZ.canvas.height);//draws the image from the imageCanvas
            
            //Cosshair
            var x = a.ctxZ.canvas.width / 2;
            var y = a.ctxZ.canvas.height / 2;

            a.ctxZ.moveTo(x, y);
            a.ctxZ.lineTo(x, y + scale);
            a.ctxZ.lineTo(x + scale, y + scale);
            a.ctxZ.lineTo(x + scale, y);
            a.ctxZ.lineTo(x, y);
            // Line color
            a.ctxZ.strokeStyle = '#DB14C1';
            a.ctxZ.stroke();
        });
        this.ctxI.canvas.addEventListener('click', function(e) {
            let pos = a.getMousePos(a.ctxI.canvas, e);
            let data = a.ctxI.getImageData(pos.x, pos.y, 1, 1).data;
            console.log('%c HSV:' + a.RGBtoHSV(data), 'background: #' + a.fullColorHex(data) + ';');
            // let hsv = a.RGBtoHSV(data)
            let exists = false;
            for (var i = 1; i < a.colors.length&&!exists; i++) {
                if(a.arraysEqual(a.colors[i],data))
                {
                    exists=true;
                }
            }
            if(!exists)
                a.colors.push(data);
            
        });

    }
    ,
}

</script> <style scoped> .colsClass {
    padding: 0 !important;
}

.videoClass {
    text-align: center;
}

.videoClass img {
    height: auto !important;
    width: 70%;
    vertical-align: middle;
}

#Point {
    padding-top: 5px;
    text-align: center;
    color: #f4f4f4;
}

</style>