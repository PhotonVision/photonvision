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
import os

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
    "sphinx.ext.mathjax",
    "sphinx.ext.graphviz",
]

# Configure OpenGraph support

ogp_site_url = "https://docs.photonvision.org/en/latest/"
ogp_site_name = "PhotonVision Documentation"
ogp_image = "https://raw.githubusercontent.com/PhotonVision/photonvision-docs/main/source/assets/RectLogo.png"

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
    app.add_css_file("css/v4-font-face.min.css")
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
    "footer_icons": [
        {
            "name": "GitHub",
            "url": "https://github.com/photonvision/photonvision",
            "html": """
                <svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"></path>
                </svg>
            """,
            "class": "",
        },
    ],
}

suppress_warnings = ["epub.unknown_project_files"]

sphinx_tabs_valid_builders = ["epub", "linkcheck"]

# -- Options for linkcheck -------------------------------------------------

# Excluded links for linkcheck
# These should be periodically checked by hand to ensure that they are still functional
linkcheck_ignore = [R"https://www.raspberrypi.com/software/", R"http://10\..+"]

token = os.environ.get("GITHUB_TOKEN", None)
if token:
    linkcheck_auth = [(R"https://github.com/.+", token)]

# MyST configuration (https://myst-parser.readthedocs.io/en/latest/configuration.html)
myst_enable_extensions = ["colon_fence"]
