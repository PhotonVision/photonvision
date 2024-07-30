<script lang="ts">
import { LogLevel } from "@/types/SettingTypes";

export default {
    name: 'photon-log-entry',
    props: {
        index: {
            type: Number
        },
        source: {
            type: Object,
            default () {
                return {}
            }
        }
    },
    methods: {
        getLogColor (level: LogLevel): string {
            switch (level) {
                case LogLevel.ERROR:
                return "red";
                case LogLevel.WARN:
                return "yellow";
                case LogLevel.INFO:
                return "light-blue";
                case LogLevel.DEBUG:
                return "white";
            }
            return "";
        }
    }
}
</script>

<template>
    <div :class="[getLogColor(source.level) + '--text', 'log-item']">
        {{ source.message }}
    </div>
</template>