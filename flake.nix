{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/064cbb73bde0942c960e0f40575339893788d45e";
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

      shellHook = "exec ${pkgs.fish}/bin/fish";
    };
  };
}