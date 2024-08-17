# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
# import os
# import sys
# sys.path.insert(0, os.path.abspath('.'))

# -- Project information -----------------------------------------------------

project = "PhotonVision"
copyright = "2024, PhotonVision"
author = "Banks Troutman, Matt Morley"

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    "sphinx_rtd_theme",
    "sphinx.ext.autosectionlabel",
    "sphinx.ext.todo",
    "sphinx_tabs.tabs",
    "notfound.extension",
    "sphinxext.remoteliteralinclude",
    "sphinxext.opengraph",
    "sphinxcontrib.ghcontributors",
    "sphinx_design",
    "myst_parser",
]

# Configure OpenGraph support

ogp_site_url = "https://docs.photonvision.org/en/latest/"
ogp_site_name = "PhotonVision Documentation"
ogp_image = "https://raw.githubusercontent.com/PhotonVision/photonvision-docs/master/source/assets/RectLogo.png"

# Add any paths that contain templates here, relative to this directory.
templates_path = ["_templates"]

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = []

# Enable hover content on glossary term
hoverxref_roles = ["term"]

# Autosection labels prefix document path and filename
autosectionlabel_prefix_document = True

# -- Options for HTML output -------------------------------------------------

html_title = "PhotonVision Docs"

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
html_theme = "furo"
html_favicon = "assets/RoundLogo.png"

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ["_static"]

source_suffix = [".rst", ".md"]


def setup(app):
    app.add_css_file("css/pv-icons.css")


pygments_style = "sphinx"

html_theme_options = {
    "sidebar_hide_name": True,
    "light_logo": "assets/PhotonVision-Header-onWhite.png",
    "dark_logo": "assets/PhotonVision-Header-noBG.png",
    "light_css_variables": {
        "font-stack": "-apple-system, BlinkMacSystemFont, avenir next, avenir, segoe ui, helvetica neue, helvetica, Ubuntu, roboto, noto, arial, sans-serif;",
        "admonition-font-size": "1rem",
        "admonition-title-font-size": "1rem",
        "color-background-primary": "#ffffff",
        "color-background-secondary": "#f7f7f7",
        "color-background-hover": "#efeff400",
        "color-background-hover--transparent": "#efeff400",
        "color-brand-primary": "#006492",
        "color-brand-content": "#006492",
        "color-foreground-primary": "#2d2d2d",
        "color-foreground-secondary": "#39a4d5",
        "color-foreground-muted": "#2d2d2d",
        "color-foreground-border": "#ffffff",
        "color-background-border": "ffffff",
        "color-api-overall": "#101010",
    },
    "dark_css_variables": {
        "color-background-primary": "#242c37",
        "color-background-secondary": "#006492",
        "color-background-hover": "#efeff400",
        "color-background-hover--transparent": "#efeff400",
        "color-brand-primary": "#ffd843",
        "color-brand-secondary": "#39a4d5",
        "color-brand-content": "#ffd843",
        "color-foreground-primary": "#ffffff",
        "color-foreground-secondary": "#ffffff",
        "color-foreground-muted": "#ffffff",
        "color-foreground-border": "transparent",
        "color-background-border": "transparent",
        "color-api-overall": "#101010",
        "color-inline-code-background": "#0d0d0d",
    },
}

suppress_warnings = ["epub.unknown_project_files"]

sphinx_tabs_valid_builders = ["epub", "linkcheck"]

# Excluded links for linkcheck
# These should be periodically checked by hand to ensure that they are still functional
linkcheck_ignore = ["https://www.raspberrypi.com/software/"]
