<template>
    <div v-on:camera-loaded="init">  
        <CVrangeSlider v-model="value.hue" name="Hue" :min="0" :max="180" @input="handleData('hue')"/>
        <CVrangeSlider v-model="value.saturation" name="Saturation" :min="0" :max="255" @input="handleData('saturation')"/>
        <CVrangeSlider v-model="value.value" name="Value" :min="0" :max="255" @input="handleData('value')"/>
        <v-divider color="darkgray "/>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setMode(1)">
                    <v-icon>colorize</v-icon>
                    Eye drop
                </v-btn>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setMode(2)">
                <v-icon>add</v-icon>
                Expand Selection
                </v-btn>
        <v-btn style="margin: 20px;" tile color="#4baf62" @click="setMode(3)">
            <v-icon>remove</v-icon>
            Shrink Selection
        </v-btn>
        <!-- <CVswitch v-model="driverState.isDriver" name="Driver Mode" @input="sendDriverMode"/> -->
        <v-divider color="darkgray "/>
        <CVswitch v-model="value.erode" name="Erode" @input="handleData('erode')"/>
        <CVswitch v-model="value.dilate" name="Dilate" @input="handleData('dilate')"/>
        <canvas id="canvas" style="display:none;" />
    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVswitch from '../../components/cv-switch'

    export default {
        name: 'Threshold',
        props:['value'],
        components:{
            CVrangeSlider,
            CVswitch
        },
        data() {
            return {
                mode:0,//0 none,1 eyedrop, 2 expand,3 shrink                
            }
        },
        computed: {  
            pipeline: {
                get() {
                    return this.$store.state.pipeline;
                }
            },   
            driverState: {
                get() {
                    return this.$store.state.driverMode;
                },
                set(val) {
                    this.$store.commit("driverMode", val);
                }
            }    
        },
        methods:{  
            init()
            {
                console.log("init");
                let img = document.getElementById('CameraStream');
                img.crossOrigin = 'Anonymous';
                img.setAttribute('crossOrigin', '');
                let x,y;
                let ref = this;
                img.addEventListener('click', function (evt) {
                        let rect = img.getBoundingClientRect();
                        x = Math.round(evt.clientX - rect.left);
                        y = Math.round(evt.clientY - rect.top);
                        let canvas = document.getElementById('canvas');
                        canvas.width = img.width;
                        canvas.height = img.height;
                        canvas.getContext('2d').drawImage(img, 0, 0, img.width, img.height);
                        let pixelData = canvas.getContext('2d').getImageData(x, y, 1, 1).data;//Creating a canvas to get the pixel color, i wish there was a better way to do it
                        switch (ref.mode)
                        {
                            case 1: ref.eyeDrop(pixelData);break;
                            case 2: ref.expand(pixelData);break;
                            case 3: ref.shrink(pixelData);break;
                        }
                    });
            },                  
            eyeDrop(pixel) {
                console.log("eye droppping on "+pixel);
                let hsv = this.RGBtoHSV(pixel);
                let range = this.createRange([hsv]);  
                range = this.widenRange(range);
                this.setRange(range);
                this.setMode(0);
            },
            expand(pixel) {
                console.log("expanding on "+pixel);
                let hsv = this.RGBtoHSV(pixel);
                let widenHSV = this.widenRange([[].concat(hsv),hsv]);
                let range = this.createRange(this.getRange().concat(widenHSV));
                this.setRange(range);
                this.setMode(0);
            },
            shrink(pixel) {
                let hsv = this.RGBtoHSV(pixel);
                let widenHSV = this.widenRange([[].concat(hsv),hsv]);
                let range = this.getRange();
                if(!this.shrinkRange(range,widenHSV[0]))
                    this.shrinkRange(range,widenHSV[1]);
                console.log(range);
                this.setRange(range);
                this.setMode(0);
            },
            //Sets driver mode on when m is not zero (aka while choosing a color)
            setMode: function(m)
            {
                this.mode=m;
                this.driverState.isDriver=(m!==0);
                this.handleInput('driverMode', this.driverState);
                this.$emit("update");
            },

            //----------------------------------------------
            //Color utils
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

            //----------------------------------------------
            //Range utils

            createRange: function(HSVColors)
            {
                let range = [[],[]];
                for (var i = 0; i < 3; i++) {
                    range[0][i]=HSVColors[0][i];
                    range[1][i]=HSVColors[0][i];
                    for (var j = HSVColors.length - 1; j >= 0; j--) {
                        range[0][i]=Math.min(HSVColors[j][i],range[0][i]);
                        range[1][i]=Math.max(HSVColors[j][i],range[1][i]);
                    }
                }
                return range;//[[Hmin,Smin,Vmin],[Hmax,Smax,Vmax]]
            },

            //This function adds 10 extra units to each side of the sliders, not to be confued with the expand selection button
            widenRange(range)
            {
                let expanded = [[],[]]
                for (var i = 0; i < 3; i++) {
                    //Expanding the range by 10
                    expanded[0][i]=Math.max(0, range[0][i]-10);               
                    expanded[1][i]=Math.min(255, range[1][i]+10);
                }
                expanded[1][0]=Math.min(180,expanded[1][0]);//h is up to 180
                return expanded;
            },

            //If color in range then take the closer range value to color and set it to color plus or minus 10
            //For example if hmax is 200 hmin is 100 and color's h is 120 range will become [130,200]
            shrinkRange(range,color)
            {
                let inside = true;
                for(let i =0;i<color.length&&inside;i++)
                {
                    if(!(range[0][i]<=color[i]<=range[1][i]))
                        inside=false;
                }                
                if(inside)
                {
                    for(let j =0;j<color.length;j++){
                        if(color[j]-range[0][j]<range[1][j]-color[j])
                            range[0][j]=Math.min(range[0][j]+10,range[1][j])//shrink from min side
                        else
                            range[1][j]=Math.max(range[1][j]-10,range[0][j])//shrink from max side
                        
                    }
                }
                console.log("range"+range);  
                return inside;//returns if color is inside or not
            },

            //applies the range to store
            setRange(range)
            {
                this.value.hue = [range[0][0],range[1][0]];
                this.handleData('hue');
                this.value.saturation = [range[0][1],range[1][1]];
                this.handleData('saturation');
                this.value.value = [range[0][2],range[1][2]];                
                this.handleData('value');
            },

            //returns the current range in format [[Hmin,Smin,Vmin],[Hmax,Smax,Vmax]]
            getRange(){
                let a= this.value;
                return [[a.hue[0],a.saturation[0],a.value[0]],[a.hue[1],a.saturation[1],a.value[1]]];
            },
            handleData(val){
                this.handleInput(val,this.value[val]);
                this.$emit('update')
            },     
        },
        mounted: function()
        {            
            let a = this.init;
            window.onload = function() {
            if(document.getElementById('CameraStream'))
              a();//this.init();
        }
    }
}

</script>

<style lang="" scoped>
    
</style>