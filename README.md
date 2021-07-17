# high-frequency-trading
We propose a framework for studying optimal market-making policies in a limit order book


<html>
<head>
  
</head>
<body>
  
<h1>Input Data BTC-USD</h1>
<div align="justify">
The input data used for this specific case can be found here
<a href="https://bitdataset.com/historical-data/binance-usd-m-futures-top-of-book-quotes-data/">Input data</a> <br /> 
</div>
  
<h1>Input Model Parameters</h1>
  
<table>
    <thead>
        <tr>
            <th><sub>Name</sub></th>
            <th><sub>Value</sub></th>
        </tr>
    </thead>
    <tbody>

      <tr><td><sub>final wealth</sub></td>    <td><sub>333.71</sub></td></tr>
      
    </tbody>
</table>




<h1>Best Policy Results</h1>
  
Market orders best policy (Take) -time=100s
<p align="center">
  <img width="1200" height="600" src="src/main/resources/output/take_vol.png">
</p>

Limit orders best policy - Bid -time=100s
<p align="center">
  <img width="1200" height="600" src="src/main/resources/output/bid_vol_100.png">
</p>

Limit orders best policy - Ask -time=100s
<p align="center">
  <img width="1200" height="600" src="src/main/resources/output/ask_vol_100.png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(1).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(2).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(3).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(4).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(5).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(6).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(7).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(8).png">
</p>

<p align="center">
  <img width="1200" height="500" src="src/main/resources/output/newplot(9).png">
</p>
 
<table>
    <thead>
        <tr>
            <th><sub>Name</sub></th>
            <th><sub>Mean</sub></th>
            <th><sub>SD</sub></th>
        </tr>
    </thead>
    <tbody>

<tr><td><sub>final wealth</sub></td>    <td><sub>333.71</sub></td>          <td><sub>342.08</sub></td></tr>
<tr><td><sub>num_best_ask_orders</sub></td>          <td><sub>60.10</sub></td>    <td><sub>17.81</sub></td></tr>
<tr><td><sub>num_new_ask_orders</sub></td>           <td><sub>18.49</sub></td>     <td><sub>20.06</sub></td></tr>
<tr><td><sub>num_best_bid_orders</sub></td>          <td><sub>61.33</sub></td>    <td><sub>17.30</sub></td></tr>
<tr><td><sub>num_new_bid_orders</sub></td>           <td><sub>17.10</sub></td>    <td><sub>17.50</sub></td></tr>
<tr><td><sub>num_market_buy_orders</sub></td>        <td><sub>18.42</sub></td>    <td><sub>11.58</sub></td></tr>
<tr><td><sub>num_market_sell_orders</sub></td>        <td><sub>0.69</sub></td>    <td><sub>1.32</sub></td></tr>
<tr><td><sub>max_inventory</sub></td>             <td><sub>343.83</sub></td>    <td><sub>411.17</sub></td></tr>
<tr><td><sub>min_inventory</sub></td>            <td><sub>-857.19</sub></td>    <td><sub>705.33</sub></td></tr> 
      
</tbody>
 </table>

<table>
    <tbody>
     <tr><td><sub>Information ratio</sub></td>    <td><sub>0.97</sub></td> 
  </tbody>
</table>

<h1>Contact</h1>
<div align="justify">
E-Mail Adresse: ste.penazzi1987@gmail.com <br />
<a href="https://www.linkedin.com/in/stefano-penazzi-datascientist/">LinkedIn</a> <br />
</div>


</body>
</html>
