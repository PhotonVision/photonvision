<template>
  <div id="app">
      <div class="layout">
        <Layout :style="{minHeight: '100vh'}">
            <Sider collapsible :collapsed-width="78" v-model="isCollapsed">
                <Menu @on-open-change="onOpenChange" active-name="vision-input" :open-names="['vision']"  width="auto" :class="menuitemClasses">
                    <Submenu name="vision">
                      <template slot="title">
                        <Icon type="ios-videocam"/>
                        <span v-if="!isCollapsed">Vision</span>
                      </template>
                      <MenuItem name="vision-input">Input</MenuItem>
                      <MenuItem name="vision-3D">3D</MenuItem>
                    </Submenu>
                    <Submenu name="settings">
                      <template slot="title">
                        <Icon type="ios-settings"/>
                        <span v-if="!isCollapsed">Settings</span>
                      </template>
                      <MenuItem name="settings-color">Color</MenuItem>
                      <MenuItem name="settings-brightness">Brightness</MenuItem>
                    </Submenu>
                </Menu>
            </Sider>
        </Layout>
    </div>
  </div>
</template>

<script>

export default {
  name: 'app',
  data () {
    return {
        isCollapsed: false
    };
  },
  methods: { 
    onOpenChange() {
      this.isCollapsed = false;
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

.layout-con{
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
