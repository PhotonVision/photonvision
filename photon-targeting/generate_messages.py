#!/usr/bin/env python3

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, TypedDict, cast

import yaml
from jinja2 import Environment, FileSystemLoader
from jinja2.environment import Template


class SerdeField(TypedDict):
    name: str
    type: str

class MessageType(TypedDict):
    name: str
    fields: List[SerdeField]


def yaml_to_dict(path: str):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    yaml_file_path = os.path.join(script_dir, path)

    with open(yaml_file_path, "r") as file:
        file_dict: List[MessageType] = yaml.safe_load(file)

    # Print for testing
    print(file_dict)

    return file_dict


data_types = yaml_to_dict("src/generate/message_data_types.yaml")

def parse_yaml():
    config = yaml_to_dict("src/generate/messages.yaml")

    # Hash a comments-stripped version for message integrity checking
    cleaned_yaml = yaml.dump(config, default_flow_style=False).strip()
    message_hash = hashlib.md5(cleaned_yaml.encode("ascii")).digest()
    message_hash = list(message_hash)
    print(message_hash)

    return config, message_hash


def generate_photon_messages(output_root, template_root):
    messages, message_hash = parse_yaml()

    env = Environment(
        loader=FileSystemLoader(str(template_root)),
        # autoescape=False,
        # keep_trailing_newline=False,
    )


    # add our custom types
    for message in messages:
        name = message['name']
        data_types[name] = {
            'len': -1,
            'java_type': name,
            'cpp_type': name,
        }

    root_path = Path(output_root) / "main/java/org/photonvision/struct"
    template = env.get_template("Message.java.jinja")

    root_path.mkdir(parents=True, exist_ok=True)

    for message in messages:
        java_name = f"{message['name']}Serde.java"
        

        output_file = root_path / java_name
        output_file.write_text(template.render(message, type_map=data_types), encoding="utf-8")



def main(argv):
    script_path = Path(__file__).resolve()
    dirname = script_path.parent

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output_directory",
        help="Optional. If set, will output the generated files to this directory, otherwise it will use a path relative to the script",
        default=dirname / "src/generated",
        type=Path,
    )
    parser.add_argument(
        "--template_root",
        help="Optional. If set, will use this directory as the root for the jinja templates",
        default=dirname / "src/generate",
        type=Path,
    )
    args = parser.parse_args(argv)

    generate_photon_messages(args.output_directory, args.template_root)


if __name__ == "__main__":
    main(sys.argv[1:])
