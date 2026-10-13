# Project icon

The current icon composes existing **iron and copper vault textures** into two stacked vaults. It uses integer nearest-neighbor projection and a transparent background; no generative image model or previous AI-generated logo is used as a bitmap input.

![Texture-based project icon](assets/project-icon.png)

## Source and license

The six input faces are the installed 16×16 front, side and top vault textures. Their original bitmap source is Create 6.0.4 artwork, recolored for this project. The original Create MIT notice is retained in [LICENSE-Create.txt](../src/main/resources/META-INF/LICENSE-Create.txt).

[icon-provenance.json](assets/icon-provenance.json) records input paths and SHA-256 values, original Create texture paths, native and output dimensions, polygon placements, outlines, and output hashes. The same 400×400 PNG is used for the project image and `src/main/resources/pack.png`.

The previous logo was derived from an AI-generated image. It remains in historical release artifacts and a local backup, and is not an input to this icon.

## Rendering recipe

1. Start with an 80×80 transparent RGBA canvas.
2. Read each face listed in the provenance file as a 16×16 RGBA image.
3. Process the recorded faces in their given order. For each destination pixel center, solve its coordinates in the recorded parallelogram using the first, second and fourth vertices; sample the source with integer nearest-neighbor coordinates.
4. Draw each cube's face boundaries with the recorded one-pixel outline after its three faces have been filled. Draw the iron cube before the copper cube.
5. Resize to 400×400 using nearest-neighbor sampling and save a lossless RGBA PNG to both recorded output paths.

Python/Pillow was used for the transform and composition. The instructions and arrangement were AI-assisted; this record does **not** claim human-only authorship or guarantee platform approval. Platform AI disclosures and image rules must be reviewed separately before publication.

## Release boundary

This replacement belongs to the current development tree. Published 1.1.1, 1.1.2 and 1.2.0 JARs retain their original bytes and icon. A future binary release needs its own version and tag; replacing an existing release attachment is not part of this change.
