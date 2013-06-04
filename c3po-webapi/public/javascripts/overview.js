$(document).ready(
		function() {
			var button = $('<a  href="#" class="green_button">Add Diagram</a>').appendTo($('#more'));
			var buttonBubbleCharts = $('<a  href="#" class="green_button">Add Bubble Chart</a>').appendTo($('#bubble'));
			button.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showPopup(oData);

					}
				});
			});
			
			buttonBubbleCharts.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showBubbleChartPopup(oData);

					}
				});
			});

		});


function showPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select a property');
	var config = popup.children('.popupconfig');

	var sel = $('<select>').appendTo($(config));
	$(sel).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel).append($('<option>').text(value).attr('value', value));
	});

	popup.css({
		'display' : 'block',
		'z-index' : 11
	});

	$('.popupconfig select').change(function() {
		$.ajax({
			type : 'GET',
			url : '/c3po/property?name=' + $(this).val(),
			timeout : 5000,
			success : function(oData) {
				showOptions(oData.type, false);
			}
		});
	});
};


function showBubbleChartPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select a property pair.');
	var config = popup.children('.popupconfig');

	// property 1 selection
	var row1 = $('<div />').appendTo($(config));
	
	var sel = $('<select id="prop1">').appendTo($(row1));
	$(sel).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel).append($('<option>').text(value).attr('value', value));
	});

	$(config).append($('<br />'));

	// property 2 selection
	var row2 = $('<div />').appendTo($(config));

	var sel2 = $('<select id="prop2">').appendTo($(row2));
	$(sel2).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel2).append($('<option>').text(value).attr('value', value));
	});
	
	popup.css({
		'display' : 'block',
		'z-index' : 11
	});

	// TODO ALEX change to automatically generated ids for more property pairs
	// on change of one property select
	$('.popupconfig select').change(function() {
		var select = $(this);
		var value = $(select).val();
		// build ids 
		var thisId = $(select).attr("id");
		var algId = thisId + "_alg";
		var widthId = thisId + "_width";
		// reset selction 
		$(select).removeData();	// remove stored values
		$('#' + algId).remove();
		$('#' + widthId).remove();
		
		if (value) {
			// get property (name, type...)
			$.ajax({
				type : 'GET',
				url : '/c3po/property?name=' + value,
				timeout : 5000,
				success : function(oData) {
					// show width method and selection if this is a numeric property
					$(select).data('type', oData.type);
					if (oData.type == "INTEGER" || oData.type == "FLOAT") {
    					$(select).parent().append($(
    						'<select id=\"' + algId + '\"><option/><option value="fixed">fixed</option><option value="sturge">Sturge\'s</option><option value="sqrt">Square-root choice</option></select>'));
					}
					$('#' + algId).change(function() {
						$(select).data('alg', $(this).val());
						if ($(this).val() == "fixed") {
							$('#' + algId).parent().append($('<input id=\"' + widthId + '\" type="text" placeholder="bin width" />'));
						}
					});
				}
			});
		}
	});
	
	$(config).append($('<br />'));
	$(config).append($('<br />'));

	// apply button
	var row3 = $('<div align="right" style="padding-right: 3em;" />').appendTo($(config));
	var apply = $('<a class="green_button" href="#" >apply</a>').appendTo($(row3));
	apply.click(function() {
		// input validation
		if ($('#prop1').val() == $('#prop2').val()) {
			alert("please select different properties");
			return;
		}
		
		// build url and check input values
		var url = "/c3po/overview/bubblegraph?";
		for (var i = 1; i <= 2; i++) {
    		var type = $('#prop' + i).data('type');
    		if (!type) {
    			alert("no value for property " + i + " selected");
    			return;
    		}
    		url += "property" + i + "=" + $('#prop' + i).val();
    		if (type == "INTEGER" || type == "FLOAT") {
    			var alg = $('#prop' + i).data('alg');
    			if (!alg) {
    				alert("no bin width method selected for numeric property " + i);
    				return;
    			}
    			url += "&alg" + i + "=" + alg;
    			if (alg == "fixed") {
    				var width = $('#prop' + i + '_width').val();
    				if (!$.isNumeric(width)) {
    					alert("given bin width for numeric property " + i + " is not a valid number");
    					return;
    				}
    				url += "&width" + i + "=" + width;
    			}
    		}
    		url += "&";
		}
		
		// we have all needed values, do the graph fetching
		startSpinner();
		$.ajax({
			type : 'GET',
			url : url,
			timeout : 5000,
			success : function(oData) {
				hidePopupDialog();
    			var data = {};
    			data[oData.title] = {
    			                     type: oData.type,
    			                     data: oData.graphData,
    			                     options: oData.graphOptions
    			                     };
    			
    			drawGraphs(data);
    			stopSpinner();
    		} // end success function
    	}); // end ajax call
	}); // end apply.click 

};

function showOptions(type, bubble) {
	if (type == "STRING" || type == "BOOL" || type == "DATE") {
		var property = $('.popupconfig select').val();
		hidePopupDialog();
		startSpinner();
		$.ajax({
			type : 'GET',
			url : '/c3po/overview/graph?property=' + property,
			timeout : 5000,
			success : function(oData) {
				stopSpinner();
				var hist = [];
				$.each(oData.keys, function(i, k) {
					hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
				});
				var id = oData.property;
				var data = {};
				data[id] = {
				         type: 'histogram',
				         data: hist,
				         options: null
				         };
				
				
				drawGraphs(data);
				//scroll to bottom of page.

			}
		});

	} else {
		if(!bubble) {
			showIntegerPropertyDialog('applyIntegerHistogramSelection()');
		}
		else {
			var alg = "sqrt";
			var width = -1;
		}
		
	}
}

function applyIntegerHistogramSelection() {
	var selects = $('.popupconfig').children('select');
	var property = $('.popupconfig').children('select:first').val();
	var alg = $('.popupconfig').children('select:last').val();
	var width = -1;
	if (alg == "fixed") {
		width = $('.popupconfig input:first').val();
	}

	hidePopupDialog();
	startSpinner();
	$.ajax({
		type : 'GET',
		url : '/c3po/overview/graph?property=' + property + "&alg=" + alg
				+ "&width=" + width,
		timeout : 5000,
		success : function(oData) {
			var hist = [];
			$.each(oData.keys, function(i, k) {
				hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
			});
			var id = oData.property;
			var data = {};
			data[id] = hist;
			$('#' + id).remove(); // remove the old graph if exist
			drawGraphs(data, oData.options);
			stopSpinner();
			//scroll to bottom of page.
		}
	});
};

function getBarChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.BarRenderer,
			// Show point labels to the right ('e'ast) of each bar.
			// edgeTolerance of -15 allows labels flow outside the grid
			// up to 15 pixels. If they flow out more than that, they
			// will be hidden.
			pointLabels : {
				show : true,
				location : 'n',
				edgeTolerance : -15
			},
			// Rotate the bar shadow as if bar is lit from top right.
			shadowAngle : 70,
			// Here's where we tell the chart it is oriented horizontally.
			rendererOptions : {
				barDirection : 'vertical',
				barWidth : '12'
			},
			color : '#639B00'
		},
		axesDefaults : {
			tickRenderer : $.jqplot.CanvasAxisTickRenderer,
			tickOptions : {
				angle : -30,
				fontSize : '8pt'
			}
		},
		axes : {
			// Use a category axis on the x axis and use our custom ticks.
			xaxis : {
				renderer : $.jqplot.CategoryAxisRenderer,
				tickOptions : {
					formatter : function(format, val) {
						if (val.length > 30) {
							val = val.substring(0, 25) + '...';
						}

						// val = (val.replace(/\.0/g, ""));
						return val;
					}
				}
			},
			// Pad the y axis just a little so bars can get close to, but
			// not touch, the grid boundaries. 1.2 is the default padding.
			yaxis : {
				pad : 1.05,
				tickOptions : {
					formatString : '%d',
				}
			}
		},
		highlighter : {
			show : true,
			tooltipLocation : 'n',
			showTooltip : true,
			useAxesFormatters : true,
			sizeAdjust : 0.5,
			tooltipAxes : 'y',
			bringSeriesToFront : true,
			tooltipOffset : 30,
		},
		cursor : {
			style : 'pointer', // A CSS spec for the cursor type to change the
								// cursor to when over plot.
			show : true,
			showTooltip : false, // show a tooltip showing cursor position.
			useAxesFormatters : true, // wether to use the same formatter and
										// formatStrings
		// as used by the axes, or to use the formatString
		// specified on the cursor with sprintf.
		}

	};

	return options;
};

function getPieChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.PieRenderer,
			rendererOptions : {
				showDataLabels : true
			}
		},
		legend : {
			show : true,
			location : 'e'
		}
	};

	return options;
};

function getBubbleChart(ttl) {
	var options = {
			title : ttl,
			seriesDefaults : {
		           renderer : $.jqplot.BubbleRenderer,
		           rendererOptions : {
		               bubbleAlpha : 0.6,
		               highlightAlpha : 0.8,
		               varyBubbleColors : false,
		               color : '#639B00',
		               bubbleGradients: true,
		               showLabels: false
		           },
		           shadow : true,
		           shadowAlpha : 0.05,
		           
			},
			highlighter : {
				show : true,
				tooltipLocation : 'n',
				showTooltip : true,
				useAxesFormatters : true,
				sizeAdjust : 0.5,
				tooltipAxes : 'xy',
				bringSeriesToFront : true,
				tooltipOffset : 50,
			},
			cursor : {
				style : 'pointer', 
				show : true,
				showTooltip : false,
				useAxesFormatters : true,
			}
		};
		return options;
}

function prettifyTitle(title) {
	title = title.replace(/_/g, " ");
	return title.replace(/\w\S*/g, function(txt) {
		return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
	});
};

function drawGraphs(data, options) {
	var idx = 0;
	var graphsdiv = $('#graphs');
	$.each(data, function(i, d) {
		var container;
		var clazz;
		if (idx % 2 == 0) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_left";
		} else if (idx % 2 == 1) {
			container = graphsdiv.children('.span-24:last');
			clazz = "dia_right";
		}

		if (d.data.length > 30) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_full";
			idx++; // if full length skip to next row left
		}

		container.append('<div id="' + i + '" class="' + clazz + '">');
		$('#' + i).bind(
				'jqplotDataClick',
				function(ev, seriesIndex, pointIndex, data) {
					startSpinner();
					var url = '/c3po/filter?filter=' + i + '&value='
							+ pointIndex + '&type=graph';

					if (options) {
						var type = options['type'];
						var alg = options['alg'];
						var width = options['width'];

						if (type == 'INTEGER') {
							url += '&alg=' + alg;

							if (width) {
								url += '&width=' + width;
							}
						}
					}
					$.post(url, function(data) {
						window.location = '/c3po/overview';
					});
				});
		if (d.type == "histogram") {		
		  $.jqplot(i, [ d.data ], getBarChart(prettifyTitle(i)));
		} else if (d.type == "bubblechart") {
		  // draw a bubble chart
		  $.jqplot(i, [ d.data ], getBubbleChart(prettifyTitle(i)));
		}

		if (idx == 0) {
			idx++; // if first row skip the right and go to next row...
		}
		idx++;
	})
};


function drawBubbleChart(title) {
	ttl="bubbleChart";
	if($("#bubbleCh").length > 0) {
		$("#bubbleCh").remove();
	}
	// TODO ALEX append instead of prepend once testing is done
	var container = $('<div id="bubbleCh" class="span-24">');
	var graphsdiv = $('#graphs').prepend(container);
	
	var data = 
		[[11, 123, 1236, "Acura"], [45, 92, 1067, "Alfa Romeo"],
		 [24, 104, 1176, "AM General"], [50, 23, 610, "Aston Martin Lagonda"],
		 [18, 17, 539, "Audi"], [7, 89, 864, "BMW"], [2, 13, 1026, "Bugatti"]];
	            
	
	clazz="";
	container.append('<div id="' + ttl + '" class="' + clazz + '">');
	
	$.jqplot(ttl, [ data ], getBubbleChart(prettifyTitle(ttl)));

   /*
   $.each(data, function(i, d) {

		$('#' + i).bind(
				'jqplotDataClick',
				function(ev, seriesIndex, pointIndex, data) {
					startSpinner();
					var url = '/c3po/filter?filter=' + i + '&value='
							+ pointIndex + '&type=graph';

					
					$.post(url, function(data) {
						window.location = '/c3po/overview';
					});
				});

	})*/
};


