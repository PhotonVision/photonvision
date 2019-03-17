  <template>
    <div id="app">
      <div class="layout">
          <Layout :style="{minHeight: '100vh'}">
            <Layout>
              <Sider id="main-nav" @on-collapse="onCollapse"  collapsible :collapsed-width="78" v-model="isCollapsed">
                    <Menu ref="menu" @on-open-change="onOpenChange" :active-name="activeName" :open-names="openedNames" theme="dark"  width="auto" :class="menuitemClasses">
                        <Submenu name="vision">
                          <template slot="title">
                            <Icon type="ios-videocam"/>
                            <span v-if="!isCollapsed">Vision</span>
                          </template>
                          <MenuItem name="vision-input" to="/vision/input">Input</MenuItem>
                          <MenuItem name="vision-3D" to="/vision/3d">3D</MenuItem>
                        </Submenu>
                        <Submenu name="settings">
                          <template slot="title">
                            <Icon type="ios-settings"/>
                            <span v-if="!isCollapsed">Settings</span>
                          </template>
                          <MenuItem name="settings-color">System</MenuItem>
                          <MenuItem name="settings-brightness">Cameras</MenuItem>
                        </Submenu>
                    </Menu>
                </Sider>
                <Layout id="main-layout">
                  <Header id="main-header">Header</Header>
                  <Content id="main-content">
                    <row type="flex" justify="start" align="middle" :gutter="5" >
                        <i-col span="12">
                        <router-view></router-view>
                        </i-col>
                        <i-col span="12">
                          <img src="./assets/logo.png">
                      </i-col>
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

  export default {
    name: 'app',
    data () {
      return {
          isCollapsed: false,
          openedNames: ['vision'],
          activeName: "vision-input"
      };
    },
    methods: { 
      onOpenChange(data) {
        this.isCollapsed = false;
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