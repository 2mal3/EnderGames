{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable-small";
  };

  outputs = {
    self,
    nixpkgs,
  }: let
    pkgs = nixpkgs.legacyPackages."x86_64-linux";
  in {
    devShells."x86_64-linux".default = pkgs.mkShell {
      packages = with pkgs; [
        papermc
      ];

      EG_DEBUG = "true";

      shellHook = "exec ${pkgs.fish}/bin/fish";
    };
  };
}