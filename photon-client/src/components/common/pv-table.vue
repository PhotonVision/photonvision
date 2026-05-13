<script setup lang="ts">
import { computed, useAttrs } from "vue";

defineOptions({
	inheritAttrs: false
});

const props = withDefaults(
	defineProps<{
		density?: "default" | "comfortable" | "compact";
		hover?: boolean;
		striped?: "odd" | "even";
		fixedHeader?: boolean;
		fixedFooter?: boolean;
		fixedHeight?: boolean;
		hasTop?: boolean;
		hasBottom?: boolean;
	}>(),
	{
		density: "default",
		hover: true,
		fixedHeader: false,
		fixedFooter: false,
		fixedHeight: false,
		hasTop: false,
		hasBottom: false
	}
);

const attrs = useAttrs();

const tableClasses = computed(() => [
	"pv-table",
	props.density ? `pv-table--density-${props.density}` : "",
	props.striped ? `pv-table--striped-${props.striped}` : "",
	props.hover ? "pv-table--hover" : "",
	props.fixedHeader ? "pv-table--fixed-header" : "",
	props.fixedFooter ? "pv-table--fixed-footer" : "",
	props.fixedHeight ? "pv-table--fixed-height" : "",
	props.hasTop ? "pv-table--has-top" : "",
	props.hasBottom ? "pv-table--has-bottom" : ""
]);
</script>

<template>
	<div v-bind="attrs" :class="[tableClasses, attrs.class]">
		<table>
			<slot />
		</table>
	</div>
</template>

<style>
.pv-table {
	--pv-table-header-height: 56px;
	--pv-table-row-height: 52px;
	--pv-table-border-color: rgba(var(--v-border-color), var(--v-border-opacity, 0.12));
	--pv-table-hover-color: rgba(var(--v-border-color), var(--v-hover-opacity, 0.08));
	--pv-table-stripe-color: rgba(var(--v-border-color), var(--v-hover-opacity, 0.06));
	--pv-table-background: rgb(var(--v-theme-surface));
	--pv-table-text: rgba(var(--v-theme-on-surface), var(--v-high-emphasis-opacity, 0.87));

	width: 100%;
	max-width: 100%;
	overflow: auto;
	border-radius: inherit;
	line-height: 1.5;
	transition-duration: 0.28s;
	transition-property: box-shadow, opacity, background, height;
	transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

.pv-table--density-comfortable {
	--pv-table-header-height: calc(56px - 2px);
	--pv-table-row-height: calc(52px - 2px);
}

.pv-table--density-compact {
	--pv-table-header-height: calc(56px - 4px);
	--pv-table-row-height: calc(52px - 4px);
}

.pv-table table {
	width: 100%;
	border-collapse: separate;
	border-spacing: 0;
	font-size: 0.95rem;
	color: var(--pv-table-text);
}

.pv-table thead th {
	text-align: left;
	font-weight: 500;
	font-size: 0.85rem;
	background: var(--pv-table-background);
	border-bottom: 1px solid var(--pv-table-border-color);
	padding: 0 16px;
	white-space: nowrap;
	height: var(--pv-table-header-height);
	user-select: none;
}

.pv-table tbody td,
.pv-table tfoot td,
.pv-table tfoot th {
	padding: 0 16px;
	border-bottom: 1px solid var(--pv-table-border-color);
	height: var(--pv-table-row-height);
}

.pv-table tbody tr:last-child td {
	border-bottom: 0;
}

.pv-table--hover tbody tr:hover {
	background: var(--pv-table-hover-color);
}

.pv-table--striped-even tbody tr:nth-child(even) {
	background-image: linear-gradient(0deg, var(--pv-table-stripe-color), var(--pv-table-stripe-color));
}

.pv-table--striped-odd tbody tr:nth-child(odd) {
	background-image: linear-gradient(0deg, var(--pv-table-stripe-color), var(--pv-table-stripe-color));
}

.pv-table--fixed-header thead th {
	position: sticky;
	top: 0;
	z-index: 2;
	box-shadow: inset 0 -1px 0 var(--pv-table-border-color);
}

.pv-table--fixed-footer tfoot th,
.pv-table--fixed-footer tfoot td {
	position: sticky;
	bottom: 0;
	z-index: 1;
	background: var(--pv-table-background);
	box-shadow: inset 0 1px 0 var(--pv-table-border-color);
}

.pv-table--fixed-height {
	overflow-y: auto;
}

.pv-table--has-top {
	border-top-left-radius: 0;
	border-top-right-radius: 0;
}

.pv-table--has-bottom {
	border-bottom-left-radius: 0;
	border-bottom-right-radius: 0;
}

.pv-table caption {
	text-align: left;
	padding: 0.6rem 16px;
	color: rgba(var(--v-theme-on-surface), var(--v-medium-emphasis-opacity, 0.6));
	font-size: 0.85rem;
}
</style>
