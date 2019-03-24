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
                          <MenuItem name="/vision/3d" to="/vision/3d">Threshold</MenuItem>
                        </Submenu>
                        <Submenu name="/settings">
                          <template slot="title">
                            <Icon type="ios-settings"/>
                            <span v-if="!isCollapsed">Settings</span>
                          </template>
                          <MenuItem name="/settings/color" to="/settings/system">System</MenuItem>
                          <MenuItem name="/settings/brightness" to="/settings/camera">Cameras</MenuItem>
                        </Submenu>
                    </Menu>
                </Sider>
                <Layout id="main-layout">
                  <Header id="main-header" v-if="$route.path.includes('vision')">
                    <Row type="flex" justify="start" align="middle" :gutter="10">
                      <Col span="12">
                        <chselect title="select a camera" :list="[1,2,3]"></chselect>
                      </Col>
                      <Col span="12">
                        <chselect title="select pipline" :list="[0,1,2,3,4,5,6,7,8,9]"></chselect>
                      </Col>
                    </Row>
                  </Header>
                  <Content id="main-content">
                    <row type="flex" justify="start" align="middle" :gutter="5" >
                        <Col span="12">
                        <router-view></router-view>
                        </Col>
                        <Colspan="12">
                          <img src="./assets/logo.png">
                      </Col>
                      </row>
                    </Content>
                </Layout>
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
          this.openedNames = [this.activeName.split("-")[0]];
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
    // this.$options.sockets.onmessage = (data) => console.log(data.data); // console writes recived data
  }
  }

</script>

<style>

  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
  }

  #camera, #main-layout {
    background-color: #272e35;
    /* padding: 100px 30px 30px 30px; */
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