export type HSV = [number, number, number];
export type RGBA = [number, number, number, number] | Uint8ClampedArray;

export class ColorPicker {
  public hsvData: HSV;

  constructor(pixelData: RGBA) {
    this.hsvData = this.RGBtoHSV(pixelData);
  }

  public selectedColorRange() {
    return this.widenRange([[...this.hsvData], [...this.hsvData]]);
  }

  public expandColorRange(currentRange: [HSV, HSV]) {
    const widenedHSV = this.widenRange([[...this.hsvData], [...this.hsvData]]);
    return this.createRange(currentRange.concat(widenedHSV));
  }

  public shrinkColorRange(currentRange: [HSV, HSV]) {
    const widenedHSV = this.widenRange([[...this.hsvData], [...this.hsvData]]);

    //Tries to shrink the lower part of to widened HSV
    if (!this.shrinkRange(currentRange, widenedHSV[0])) {
      //If the prev attempt failed, try to shrink the higher part of to widened HSV
      this.shrinkRange(currentRange, widenedHSV[1]);
    }

    return currentRange;
  }

  private createRange(range: HSV[]): [HSV, HSV] {
    const newRange: [HSV, HSV] = [
      [0, 0, 0],
      [0, 0, 0]
    ];
    for (let i = 0; i < 3; i++) {
      newRange[0][i] = range[0][i];
      newRange[1][i] = range[0][i];
      for (let j = range.length - 1; j >= 0; j--) {
        newRange[0][i] = Math.min(range[j][i], newRange[0][i]);
        newRange[1][i] = Math.max(range[j][i], newRange[1][i]);
      }
    }
    return newRange;
  }

  private widenRange(range: [HSV, HSV]): [HSV, HSV] {
    const expanded: [HSV, HSV] = [
      [0, 0, 0],
      [0, 0, 0]
    ];
    for (let i = 0; i < 3; i++) {
      //Expanding the range by 10
      expanded[0][i] = Math.max(0, range[0][i] - 10);
      expanded[1][i] = Math.min(255, range[1][i] + 10);
    }
    expanded[1][0] = Math.min(180, expanded[1][0]); //h is up to 180
    return expanded;
  }

  private shrinkRange(range: [HSV, HSV], color: HSV): boolean {
    for (let i = 0; i < color.length; i++) {
      if (!(range[0][i] <= color[i] && color[i] <= range[1][i])) return false;
    }

    for (let i = 0; i < color.length; i++) {
      if (color[i] - range[0][i] < range[1][i] - color[i]) {
        //shrink from min side
        range[0][i] = Math.min(range[0][i] + 10, range[1][i]);
      } else {
        //shrink from max side
        range[1][i] = Math.max(range[1][i] - 10, range[0][i]);
      }
    }

    return true;
  }

  private RGBtoHSV(rgba: RGBA): HSV {
    // Normalize RGB ranges
    let r = rgba[0],
      g = rgba[1],
      b = rgba[2];
    r = r / 255;
    g = g / 255;
    b = b / 255;

    const minRGB = Math.min(r, Math.min(g, b));
    const maxRGB = Math.max(r, Math.max(g, b));
    const d = r === minRGB ? g - b : b === minRGB ? r - g : b - r;
    const h = r === minRGB ? 3 : b === minRGB ? 1 : 5;
    let H = 30 * (h - d / (maxRGB - minRGB));
    let S = (255 * (maxRGB - minRGB)) / maxRGB;
    let V = 255 * maxRGB;
    if (isNaN(H)) H = 0;
    if (isNaN(S)) S = 0;
    if (isNaN(V)) V = 0;
    return [Math.round(H), Math.round(S), Math.round(V)];
  }
}
