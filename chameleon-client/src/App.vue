  <template>
    <div id="app">
      <div class="layout">
          <Layout :style="{minHeight: '100vh'}">
            <Layout>
              <Sider id="main-nav" @on-collapse="onCollapse"  collapsible :collapsed-width="78" v-model="isCollapsed">
                    <Menu ref="menu" @on-open-change="onOpenChange" :active-name="activeName" :open-names="openedNames" theme="dark"  width="auto" :class="menuitemClasses">
                        <Submenu name="/vision">
                          <template slot="title">
                            <Icon type="ios-videocam"/>
                            <span v-if="!isCollapsed">Vision</span>
                          </template>
                          <MenuItem name="/vision/input" to="/vision/input">Input</MenuItem>
                          <MenuItem name="/vision/threshold" to="/vision/threshold">Threshold</MenuItem>
                          <MenuItem name="/vision/contours" to="/vision/contours">Contours</MenuItem>
                          <MenuItem name="/vision/output" to="/vision/output">Output</MenuItem>
                        </Submenu>
                        <Submenu name="/settings">
                          <template slot="title">
                            <Icon type="ios-settings"/>
                            <span v-if="!isCollapsed">Settings</span>
                          </template>
                          <MenuItem name="/settings/system" to="/settings/system">System</MenuItem>
                          <MenuItem name="/settings/camera" to="/settings/camera">Cameras</MenuItem>
                        </Submenu>
                    </Menu>
                </Sider>
                <router-view></router-view>
            </Layout>
        </Layout>
    </div>
  </div>
</template>

<script>
  import Vue from "vue"
  import chselect from './components/ch-select.vue'
  
  export default {
    name: 'app',
    components:{
      chselect
    },
    data () {
      return {
          isCollapsed: false,
          openedNames: ["/" + this.$route.path.split("/")[1]],
          activeName: this.$route.path
      };
    },
    methods: { 
      onOpenChange(data) {
        this.isCollapsed = false;
        console.info('App currentRoute:', this.$router.currentRoute);
      },
      onCollapse() {
        if (this.isCollapsed) {
          this.openedNames = [''];
        } else {
          this.activeName = this.$refs.menu.currentActiveName;
          this.openedNames = ["/" + this.activeName.split("/")[1]];
        }
        this.$nextTick(function() {
          this.$refs.menu.updateOpened();
          this.$refs.menu.updateActiveName();
        })
      }
    },
    computed: {
      menuitemClasses: function () {
          return [
              'menu-item',
              this.isCollapsed ? 'collapsed-menu' : ''
          ]
      }
    },
    created () {
    this.$options.sockets.onmessage = (data) => {
      console.log(data.data);
      let message = JSON.parse(data.data);
      for (var prop in message){
        if(message.hasOwnProperty(prop)){
          this.$store.state[prop] = message[prop];
        }
      }
    } // console writes recived data
  }
  }
</script>
<style>

  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    /* color: #2c3e50; */
  }

  #camera, #main-layout {
    background-color: #272e35;
  }

  #main-nav {
    box-shadow: 2px 0px 10px black;
  }

  #main-header {
    box-shadow: 0px 2px 10px black;
    text-align: left
  }

  #main-content {
    padding: 30px
  }

  .layout{
      height: 100%;
      width: 100%;
  }
  .menu-item span{
      display: inline-block;
      overflow: hidden;
      width: 69px;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: bottom;
      transition: width .2s ease .2s;
  }
  .menu-item i{
      transform: translateX(0px);
      transition: font-size .2s ease, transform .2s ease;
      vertical-align: middle;
      font-size: 16px;
  }
  .collapsed-menu span{
      width: 0px;
      transition: width .2s ease;
  }
  .collapsed-menu i{
      transform: translateX(5px);
      transition: font-size .2s ease .2s, transform .2s ease .2s;
      vertical-align: middle;
      font-size: 22px;
  }

</style>