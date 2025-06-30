export class CalObj {
    name = null;
    units = null;
    min = null;
    max = null;
    default = null;
    value = null;

    isFullyAnnounced() {
        return this.name != null &&
               this.units != null &&
               this.min != null &&
               this.max != null &&
               this.default != null &&
               this.value != null;
    }
}