from matplotlib import pyplot as plt
import numpy as np
import pandas as pd


def graph():
    df = pd.read_csv("data/edge.txt", header=-1)
    df.reset_index(inplace=True)
    df.columns = ("a","b","c")
    df.b = df.b / 1000000
    df = df[:900]
    ax = df.b.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.30])
    ax.set_xlim([20, 125])
    plt.xlabel("Delay (ms)")
    plt.savefig("edge.pdf")
    plt.show()

    plt.figure()
    df = pd.read_csv("data/cloud.txt", header=-1)
    df.reset_index(inplace=True)
    df.columns = ("a","b","c")
    df.b = df.b / 1000000
    df = df[:900]
    ax = df.b.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.30])
    ax.set_xlim([20, 125])
    plt.xlabel("Delay (ms)")
    plt.savefig("cloud.pdf")
    plt.show()

    # plt.figure()
    # df = pd.read_csv("data/bluetooth.txt", header=-1)
    # df.reset_index(inplace=True)
    # df.columns = ("a","b","c")
    # df.b = df.b / 1000000000
    # df = df[:900]
    # df.b.hist(bins=np.arange(0, 0.2, 0.001), normed=True)
    # plt.xlabel("Delay (s)")
    # plt.savefig("cloud.pdf")
    # plt.show()

    plt.figure()
    df = pd.read_csv("data/iban.txt", header=-1)
    df.reset_index(inplace=True)
    df.columns = ("a","b","c")
    df = df[:900]
    df.c = df.c * 1000
    ax = df.c.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.30])
    ax.set_xlim([20, 125])
    plt.xlabel("Delay (ms)")
    plt.savefig("iban.pdf")
    plt.show()


    pass



graph()