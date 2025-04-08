This project implements a simulation of the **Paxos consensus algorithm** to elect a **council president** among 9 members of the Adelaide Suburbs Council. The system is designed to demonstrate **fault tolerance**, **message loss**, and **variable communication delays**, reflecting real-world voting and consensus scenarios.

## Objective
- Simulate the **Paxos voting protocol** to reach consensus despite node failures and network inconsistencies.
- Model realistic behavior like **message loss**, **latency**, and **unresponsive participants**.
- Apply distributed systems concepts in a fault-tolerant voting system.

---

## Background
Each member (M1–M9) of the council behaves differently:
- **M1**: Super responsive and reliable.
- **M2**: Often unresponsive unless at a café (i.e., randomly responsive).
- **M3**: May completely disappear from the network at times.
- **M4–M9**: Neutral, fair voters with unpredictable responsiveness.
Consensus is reached when **at least 5 out of 9 members** agree on a candidate for president.

---
