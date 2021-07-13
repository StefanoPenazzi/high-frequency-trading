import pandas as pd
import plotly.graph_objects as go

def bp_visulization(best_policy_df):
    best_policy_df = best_policy_df[best_policy_df["time"] == 290]

    fig = go.Figure(data=go.Heatmap(
        z=best_policy_df["ask_ord_vol"],
        x=best_policy_df["inventory"],
        y=best_policy_df["spread"],
        colorscale='Viridis'))
    fig.update_layout(
        title='Best Policy',
        xaxis_nticks=36)
    fig.update_xaxes(
        title_text="Inventory (USD)",
    )
    fig.update_yaxes(
        title_text="Spread (USD)",
    )
    fig.show()

if __name__ == '__main__':
    best_policy_df = pd.read_csv(
        "/home/stefanopenazzi/projects/HFT/Output/78a4b1ab-78b3-4b8e-afad-7d3ef9f791c1/bestPolicy.csv")
    bp_visulization(best_policy_df)