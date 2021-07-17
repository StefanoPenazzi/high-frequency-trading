import pandas as pd
import plotly.graph_objects as go
import plotly.express as px

def bp_visulization(best_policy_df):
    best_policy_df = best_policy_df[best_policy_df["time"] == 100]

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

def backtest_visualization(best_policy_backtest_stat_df):

    #Terminal Wealth dist

    plotHistogram(best_policy_backtest_stat_df,"cash","Cash",4000)
    plotHistogram(best_policy_backtest_stat_df, "n_best_ask_orders", "Num_best_ask_orders")
    plotHistogram(best_policy_backtest_stat_df, "n_new_ask_orders", "Num_new_ask_orders")
    plotHistogram(best_policy_backtest_stat_df, "n_best_bid_orders", "Num_best_bid_orders")
    plotHistogram(best_policy_backtest_stat_df, "n_new_bid_orders", "Num_new_bid_orders")
    plotHistogram(best_policy_backtest_stat_df, "n_market_buy_orders", "Num_market_buy_orders")
    plotHistogram(best_policy_backtest_stat_df, "n_market_sell_orders", "Num_market_sell_orders")
    plotHistogram(best_policy_backtest_stat_df, "max_inventory", "Max_inventory")
    plotHistogram(best_policy_backtest_stat_df, "min_inventory", "Min_inventory")


def plotHistogram(df,column,title,nb=50):
    fig = px.histogram(df,
                       x=column,
                       title=title,
                       nbins=nb,
                       marginal="box",
                       color_discrete_sequence=['indianred'])

    fig.update_layout(height=500, width=1000, paper_bgcolor='rgba(0,0,0,0)',
                      plot_bgcolor='rgba(0,0,0,0)',
                      )
    fig.update_layout(showlegend=True)
    fig.update_xaxes(
        tickangle=-90,
        title_text=column,
        title_standoff=25,
        nticks=20,
        showgrid=True,
        gridcolor='LightGrey',
        showline=True,
        linecolor='LightGrey',
        mirror=True,
        ticks='outside'
    )
    fig.update_yaxes(
        title_text="freq",
        title_standoff=25,
        showgrid=True,
        gridcolor='LightGrey',
        showline=True,
        linecolor='LightGrey',
        mirror=True,
        ticks='outside',
    )
    fig.show()



if __name__ == '__main__':
    best_policy_df = pd.read_csv(
        "/home/stefanopenazzi/projects/HFT/Output/1733e7ba-d0d0-4677-990d-155ad4ae6832/best_policy.csv")

    best_policy_backtest_stat_df = pd.read_csv(
        "/home/stefanopenazzi/projects/HFT/Output/1733e7ba-d0d0-4677-990d-155ad4ae6832/best_policy_backtest_stat.csv")

    bp_visulization(best_policy_df)
    backtest_visualization(best_policy_backtest_stat_df)