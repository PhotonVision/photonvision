import argparse
from time import sleep

import ntcore
from tabulate import tabulate


def list_topics(inst: ntcore.NetworkTableInstance, root: str):
    topics = inst.getTable(root).getTopics()
    subtables = inst.getTable(root).getSubTables()

    print(f"Topics under {root}")
    print(
        tabulate(
            [
                [topic.getName(), topic.getType().name, topic.getTypeString()]
                for topic in topics
            ],
            headers=["Topic Name", "Type", "Type String"],
        )
    )
    print("")
    print(f"Tables under {root}")
    print(tabulate([[table] for table in subtables], headers=["Table Name"]))
    print("")


def print_topic(inst: ntcore.NetworkTableInstance, topic: str):
    sub = inst.getTopic(topic).genericSubscribe(
        options=ntcore.PubSubOptions(sendAll=True, pollStorage=20)
    )
    print("")
    print(f"Subscribed to {topic}, typestring '{sub.getTopic().getTypeString()}'")
    print(f"Properties:")
    print(sub.getTopic().getProperties())
    print("")

    start_time = ntcore._now()
    count = 0
    while True:
        now = ntcore._now()
        new_count = len(sub.readQueue())
        count = count + new_count

        hz = count / float(max(now - start_time, 0.1) * 1e-6)

        print(f"{topic} = {sub.get().value()} (rate={hz:.1f}hz, samples={count})")
        sleep(1)


def connect(inst: ntcore.NetworkTableInstance, server: str):
    inst.stopServer()
    inst.setServer(server)
    inst.startClient4("catnt")


def main():
    parser = argparse.ArgumentParser(description="Cat a topic")
    parser.add_argument(
        "--echo", type=str, help="Fully qualified topic name", required=False
    )
    parser.add_argument(
        "--server",
        type=str,
        default="127.0.0.1",
        help="IP address of the NT4 server",
        required=False,
    )
    parser.add_argument("--list", help="List all topics", required=False)

    args = parser.parse_args()
    inst = ntcore.NetworkTableInstance.getDefault()

    connect(inst, args.server)
    # retained to keep the subscriber alive
    topicNameSubscriber = ntcore.MultiSubscriber(
        inst, ["/"], ntcore.PubSubOptions(topicsOnly=True)
    )
    sleep(1)

    while not inst.isConnected():
        sleep(0.1)

    if args.list:
        list_topics(inst, args.list)
    if args.echo:
        print_topic(inst, args.echo)


if __name__ == "__main__":
    main()
