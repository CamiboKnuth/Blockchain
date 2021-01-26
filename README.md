# Blockchain

This is a basic blockchain-based messaging app that exists to test the functionality of blockchains in general.
The idea is that each node running the app will be able to help maintain a single "valid" chain and will
be able to recognize and dismiss invalid versions of the chain.

The most valid chain is determined using the proof of work consensus model, meaning that the chain with
the most computational effort put into it will be considered valid.

In this app, each user has the ability to put computational effort into creating blocks which will be added to
the valid shared chain, but they can also add valid blocks only to their local chain, and can try to inject invalid
blocks into the chain as well. All of this serves the purpose of testing to see if all nodes can agree on a valid
chain and ignore invalid ones injected maliciously.
