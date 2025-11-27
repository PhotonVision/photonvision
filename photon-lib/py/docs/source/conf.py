import os
import sys

# This adds the 'py/' directory to the Python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))

# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = "PhotonVision"
copyright = "2025, Matt Morley, Banks Troutman"
author = "Matt Morley, Banks Troutman"

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = [
    "sphinx.ext.autodoc",
    "sphinx.ext.napoleon",  # for Google/NumPy docstrings
    "sphinx_autodoc_typehints",  # for type hints in docs
]

import os
import sys

sys.path.insert(
    0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "_stubs"))
)  # add docs stubs first so they shadow unavailable third-party packages

sys.path.insert(
    0, os.path.abspath("../../photonlibpy")
)  # adjust based on your project layout
# Mock imports that aren't available in the docs build environment so autodoc
# can import the local modules even if optional runtime deps (like wpimath)
# aren't installed. Add other names here if you see warnings for missing
# third-party packages during the build.
autodoc_mock_imports = [
    "wpilib",
]
templates_path = ["_templates"]
exclude_patterns = []


# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = "sphinx_rtd_theme"
html_static_path = ["_static"]
