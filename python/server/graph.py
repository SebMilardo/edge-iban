from matplotlib import pyplot as plt
import numpy as np
import pandas as pd


def graph():
    df_e = pd.read_csv("data/edge.txt", header=-1)
    df_e.reset_index(inplace=True)
    df_e.columns = ("a","b","c")
    df_e = df_e.b / 2000000
    df_e = df_e[:900]
    ax = df_e.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.40])
    ax.set_xlim([0, 65])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("edge.pdf")
    plt.show()

    plt.figure()
    df_c = pd.read_csv("data/cloud.txt", header=-1)
    df_c.reset_index(inplace=True)
    df_c.columns = ("a","b","c")
    df_c = df_c.b / 2000000
    df_c = df_c[:900]
    ax = df_c.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.40])
    ax.set_xlim([0, 65])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("cloud.pdf")
    plt.show()

    plt.figure()
    df_b = pd.read_csv("data/bluetooth.txt", header=-1)
    df_b.reset_index(inplace=True)
    df_b.columns = ("a","b","c")
    df_b = df_b.c * 500
    df_b = df_b[:900]
    ax = df_b.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.40])
    ax.set_xlim([0, 65])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("bt.pdf")
    plt.show()

    plt.figure()
    df_i = pd.read_csv("data/iban.txt", header=-1)
    df_i.reset_index(inplace=True)
    df_i.columns = ("a","b","c")
    df_i = df_i[:900]
    df_i = df_i.c * 500
    ax = df_i.hist(bins=np.arange(0, 125), normed=True)
    ax.set_ylim([0,0.40])
    ax.set_xlim([0, 65])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("iban.pdf")
    plt.show()

    total_edge = (df_e + df_b + df_i)

    total_cloud = (df_c + df_b + df_i)


    plt.figure()
    ax = total_cloud.hist(bins=np.arange(0, 300), normed=True)
    ax.set_ylim([0,0.121])
    ax.set_xlim([50, 121])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("total_cloud.pdf")
    plt.show()


    plt.figure()
    ax = total_edge.hist(bins=np.arange(0, 300), normed=True)
    ax.set_ylim([0,0.121])
    ax.set_xlim([50, 121])
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(linestyle="dotted")
    plt.xlabel("Delay (ms)")
    plt.ylabel("PDF")
    plt.savefig("total_edge.pdf")
    plt.show()
    pass



graph()