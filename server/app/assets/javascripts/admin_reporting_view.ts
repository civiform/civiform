import {assertNotNull} from './util'
// import google = require('google.visualization')
// import Google = require('google.visualization')

declare var applicantChartData: any[];

class AdminReportingView {
  constructor() {
    this.loadGoogleChartsJS()
      .then(() => {
        console.log('chart js loaded');
        return google.charts.load('current', {packages: ['corechart', 'bar']})
      }).then(() => {
        this.drawStacked();
      })
      .catch(error => {
        console.error("Error loading Google Charts:", error);
      });
  }

  loadGoogleChartsJS(): Promise<void> {
    return new Promise((resolve, reject) => {https://github.com/civiform/civiform
      const script = document.createElement('script');
      script.src = "https://www.gstatic.com/charts/loader.js";
      script.onload = () => resolve();
      script.onerror = (error) => reject(error);
      document.head.appendChild(script);
    });
  }

  drawChart() {
    alert('drawing chart');
  }

  drawStacked() {
    var data = new google.visualization.DataTable();
    data.addColumn('string', 'screen number');
    data.addColumn('number', 'num apps');

    data.addRows(applicantChartData);

    var options = {
      title: 'Completed Blocks in Unsubmitted Applications, Last 30 Days',
      isStacked: false,
      hAxis: {
        title: 'Screen name',
        // format: 'h:mm a',
        // viewWindow: {
        //   min: [7, 30, 0],
        //   max: [17, 30, 0]
        // }
      },
      vAxis: {
        title: 'Num applications with block completed'
      }
    };

    var chart = new google.visualization.ColumnChart(document.getElementById('chart_div')!);
    chart.draw(data, options);
  }
}

export function init() {
  new AdminReportingView()
}
