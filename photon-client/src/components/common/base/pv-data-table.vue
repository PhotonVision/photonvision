<script setup lang="ts">
import { computed, ref, useAttrs, watch } from "vue";
import {
  FlexRender,
  getCoreRowModel,
  getExpandedRowModel,
  getGroupedRowModel,
  getPaginationRowModel,
  useVueTable,
  type ColumnDef,
  type Cell,
  type ExpandedState,
  type GroupingState,
  type PaginationState,
  type Row
} from "@tanstack/vue-table";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    columns: ColumnDef<unknown, unknown>[];
    data: unknown[];
    pageSize?: number;
    pageIndex?: number;
    pageSizeOptions?: number[];
    grouping?: string[];
    manualPagination?: boolean;
    pageCount?: number;
    rowCount?: number;
    emptyText?: string;
    getRowId?: (row: unknown, index: number, parent?: Row<unknown>) => string;
    itemValue?: string | ((row: unknown) => string | number);
    showExpand?: boolean;
    expanded?: Array<string | number>;
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
    pageSize: 10,
    pageIndex: 0,
    pageSizeOptions: () => [10, 20, 50],
    grouping: () => [],
    manualPagination: false,
    emptyText: "No rows",
    showExpand: false,
    density: "default",
    hover: true,
    fixedHeader: false,
    fixedFooter: false,
    fixedHeight: false,
    hasTop: false,
    hasBottom: false
  }
);

const emit = defineEmits<{
  (e: "update:pageIndex", value: number): void;
  (e: "update:pageSize", value: number): void;
  (e: "update:grouping", value: string[]): void;
  (e: "update:expanded", value: Array<string | number>): void;
}>();

const attrs = useAttrs();

const pagination = ref<PaginationState>({
  pageIndex: props.pageIndex,
  pageSize: props.pageSize
});

const grouping = ref<GroupingState>([...props.grouping]);
const expanded = ref<ExpandedState>({});

const getRowIdValue = (row: unknown, index: number, parent?: Row<unknown>) => {
  if (props.getRowId) {
    return props.getRowId(row, index, parent);
  }

  if (typeof props.itemValue === "function") {
    return props.itemValue(row);
  }

  if (typeof props.itemValue === "string" && row && typeof row === "object") {
    const record = row as Record<string, unknown>;
    const value = record[props.itemValue];
    if (value !== undefined) {
      return String(value);
    }
  }

  return String(index);
};

const toExpandedState = (values: Array<string | number> | undefined): ExpandedState =>
  (values ?? []).reduce<ExpandedState>((acc, value) => {
    acc[String(value)] = true;
    return acc;
  }, {});

watch(
  () => props.pageIndex,
  (value) => {
    if (value !== pagination.value.pageIndex) {
      pagination.value = { ...pagination.value, pageIndex: value };
    }
  }
);

watch(
  () => props.pageSize,
  (value) => {
    if (value !== pagination.value.pageSize) {
      pagination.value = { ...pagination.value, pageSize: value };
    }
  }
);

watch(
  () => props.grouping,
  (value) => {
    const next = value ?? [];
    if (next.join("|") !== grouping.value.join("|")) {
      grouping.value = [...next];
    }
  }
);

watch(
  () => props.expanded,
  (value) => {
    if (value) {
      expanded.value = toExpandedState(value);
    }
  },
  { immediate: true }
);

const table = useVueTable({
  get data() {
    return props.data;
  },
  get columns() {
    return props.columns;
  },
  getRowId: getRowIdValue,
  state: {
    get pagination() {
      return pagination.value;
    },
    get grouping() {
      return grouping.value;
    },
    get expanded() {
      return expanded.value;
    }
  },
  onPaginationChange: (updater) => {
    pagination.value = typeof updater === "function" ? updater(pagination.value) : updater;
    emit("update:pageIndex", pagination.value.pageIndex);
    emit("update:pageSize", pagination.value.pageSize);
  },
  onGroupingChange: (updater) => {
    grouping.value = typeof updater === "function" ? updater(grouping.value) : updater;
    emit("update:grouping", grouping.value);
  },
  onExpandedChange: (updater) => {
    expanded.value = typeof updater === "function" ? updater(expanded.value) : updater;
    emit("update:expanded", Object.keys(expanded.value));
  },
  getCoreRowModel: getCoreRowModel(),
  getGroupedRowModel: getGroupedRowModel(),
  getExpandedRowModel: getExpandedRowModel(),
  getPaginationRowModel: getPaginationRowModel(),
  enableGrouping: true,
  manualPagination: props.manualPagination,
  pageCount: props.pageCount
});

const pageCount = computed(() => props.pageCount ?? table.getPageCount());
const totalRows = computed(() => props.rowCount ?? table.getPrePaginationRowModel().rows.length);
const pageStart = computed(() =>
  totalRows.value === 0 ? 0 : pagination.value.pageIndex * pagination.value.pageSize + 1
);
const pageEnd = computed(() => Math.min(totalRows.value, (pagination.value.pageIndex + 1) * pagination.value.pageSize));
const emptyColspan = computed(() => table.getVisibleLeafColumns().length);
const expandedColspan = computed(() => emptyColspan.value + (props.showExpand ? 1 : 0));
const expandedSlotColumns = computed(() =>
  props.showExpand ? [null, ...table.getVisibleLeafColumns()] : table.getVisibleLeafColumns()
);

const onPageSizeChange = (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);
  if (Number.isFinite(value)) {
    table.setPageSize(value);
  }
};

const rootClasses = computed(() => ["pv-data-table", attrs.class]);

const getCellSlotName = (cell: Cell<unknown, unknown>) => `item.${cell.column.id}`;
const makeToggleExpand = (row: Row<unknown>) => (_item?: unknown) => {
  row.toggleExpanded();
};
</script>

<template>
  <div v-bind="attrs" :class="rootClasses">
    <pv-table
      :density="density"
      :hover="hover"
      :striped="striped"
      :fixed-header="fixedHeader"
      :fixed-footer="fixedFooter"
      :fixed-height="fixedHeight"
      :has-top="hasTop"
      :has-bottom="hasBottom"
    >
      <template #default>
        <thead>
          <tr v-for="headerGroup in table.getHeaderGroups()" :key="headerGroup.id">
            <th v-if="showExpand" class="w-10"></th>
            <th v-for="header in headerGroup.headers" :key="header.id" :colspan="header.colSpan">
              <div class="flex items-center gap-2">
                <FlexRender
                  v-if="!header.isPlaceholder"
                  :render="header.column.columnDef.header"
                  :props="header.getContext()"
                />
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="table.getRowModel().rows.length === 0">
            <td :colspan="expandedColspan" class="py-6 text-center text-sm text-white/60">
              {{ emptyText }}
            </td>
          </tr>
          <template v-for="row in table.getRowModel().rows" :key="row.id">
            <tr>
              <td v-if="showExpand && !row.getIsGrouped()" class="text-center">
                <slot
                  name="item.data-table-expand"
                  :internalItem="row.original"
                  :toggleExpand="makeToggleExpand(row)"
                  :isExpanded="row.getIsExpanded()"
                >
                  <pv-button
                    size="icon"
                    variant="ghost"
                    class="text-white/70 hover:text-white"
                    @click="row.toggleExpanded()"
                  >
                    {{ row.getIsExpanded() ? "-" : "+" }}
                  </pv-button>
                </slot>
              </td>
              <td v-for="cell in row.getVisibleCells()" :key="cell.id">
                <div v-if="cell.getIsGrouped()" class="flex items-center gap-2">
                  <pv-button
                    @click="row.toggleExpanded()"
                    size="icon"
                  >
                    {{ row.getIsExpanded() ? "-" : "+" }}
                  </pv-button>
                  <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
                  <span class="text-xs text-white/60">({{ row.subRows.length }})</span>
                </div>
                <FlexRender
                  v-else-if="cell.getIsAggregated()"
                  :render="cell.column.columnDef.aggregatedCell ?? cell.column.columnDef.cell"
                  :props="cell.getContext()"
                />
                <div v-else-if="cell.getIsPlaceholder()" />
                <slot
                  v-else
                  :name="getCellSlotName(cell)"
                  :item="cell.row.original"
                  :row="cell.row"
                  :cell="cell"
                  :value="cell.getValue()"
                >
                  <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
                </slot>
              </td>
            </tr>
            <tr v-if="$slots['expanded-row'] && row.getIsExpanded() && !row.getIsGrouped() ">
              <slot
                name="expanded-row"
                :item="row.original"
                :row="row"
                :columns="expandedSlotColumns"
                :colspan="expandedColspan"
              />
            </tr>
          </template>
        </tbody>
      </template>
    </pv-table>
    <div class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm text-white/70">
      <div>Rows {{ pageStart }}-{{ pageEnd }} of {{ totalRows }}</div>
      <div class="flex flex-wrap items-center gap-3">
        <pv-select
          class="min-w-20"
          :value="pagination.pageSize"
          @change="onPageSizeChange"
          :items="pageSizeOptions.map((size) => ({ name: size.toString(), value: size }))"
          label="Rows per page"
        >
        </pv-select>
        <span class="text-xs text-white/60">Page {{ pagination.pageIndex + 1 }} of {{ pageCount }}</span>
        <div class="flex items-center gap-2">
          <pv-button size="sm" variant="ghost" :disabled="!table.getCanPreviousPage()" @click="table.previousPage()">
            Prev
          </pv-button>
          <pv-button size="sm" variant="ghost" :disabled="!table.getCanNextPage()" @click="table.nextPage()">
            Next
          </pv-button>
        </div>
      </div>
    </div>
  </div>
</template>
