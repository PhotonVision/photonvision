# { pkgs ? import <nixpkgs> {} }:
with import <nixpkgs> {};
let
  
in
pkgs.mkShell {
  buildInputs = with pkgs; [
    openjdk17
    libGL
    udev
    nodejs
    corepack
  ];

  shellHook = ''
    export JAVA_HOME=${pkgs.openjdk17}
    export LD_LIBRARY_PATH="${pkgs.udev}/lib:$LD_LIBRARY_PATH"
  '';
}
