function drawBorder(ctx)
{
    ctx.beginPath();
    ctx.lineWidth="4";
    ctx.strokeStyle="black";
    ctx.rect(0,0,1200,400);
    ctx.stroke();
}

function drawPieces(ctx, pieces, color) {
    for (var i = 0; i < pieces.length; i++) {
        var piece = pieces[i];
        //console.log(bale);
        var drawX = ((piece.x + 60) /120) * 1200;
        var drawY = ((piece.y + 20) /40) * 400;
        //console.log(drawX);
        //console.log(drawY);
        ctx.beginPath();
        ctx.fillStyle=color;
        ctx.arc(drawX, drawY, 10, 0, 2*Math.PI);
        ctx.fill();
    }
}

function drawLine(ctx, x_start, y_start, x_end, y_end) {
    ctx.beginPath();
    ctx.moveTo(x_start, y_start);
    ctx.strokeStyle="black";
    ctx.lineTo(x_end, y_end);
    ctx.stroke();
}

var y_pos = 40;

function process(data) {
    var result = JSON.parse(data)

    console.log(result);
    var refresh = parseFloat(result.refresh);
    var player1_pieces = result.player1_pieces;
    var player2_pieces = result.player2_pieces;
    var player1 = result.player1;
    var player2 = result.player2;
    var player1_score = result.player1_score;
    var player2_score = result.player2_score;
    var remaining_turns = result.remaining_turns;
    var curr_round = result.curr_round;
    
    canvas = document.getElementById('canvas');
    ctx = canvas.getContext('2d');
    
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    drawBorder(ctx);
    drawPieces(ctx, player1_pieces, "red");
    drawPieces(ctx, player2_pieces, "gray");
    drawLine(ctx, 800, 0, 800, 400);
    drawLine(ctx, 400, 0, 400, 400);
    
    timeElement = document.getElementById('time');
    timeElement.innerHTML = "<pre>" + "Remaining Turns: " + remaining_turns + "\tRound: " + curr_round + "<font color=\"red\">"+ "\n\n\nPlayer 1: " + "</font>" +  player1 + "<font color=\"gray\">"+"\tPlayer 2:" + "</font>" + player2  + "\nPlayer 1 score: " + player1_score + "\tPlayer 2 score: " + player2_score + "</pre>" ;

    return refresh;
}

var latest_version = -1;

function ajax(version, retries, timeout) {
    console.log("Version " + version);
    var xhttp = new XMLHttpRequest();
    xhttp.onload = (function() {
            var refresh = -1;
            try {
                if (xhttp.readyState != 4)
                    throw "Incomplete HTTP request: " + xhttp.readyState;
                if (xhttp.status != 200)
                    throw "Invalid HTTP status: " + xhttp.status;
                //console.log(xhttp.responseText);
                refresh = process(xhttp.responseText);
                if (latest_version < version)
                    latest_version = version;
                else refresh = -1;
            } catch (message) {
                alert(message);
            }

            console.log(refresh);
            if (refresh >= 0)
                setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
        });
    xhttp.onabort = (function() { location.reload(true); });
    xhttp.onerror = (function() { location.reload(true); });
    xhttp.ontimeout = (function() {
            if (version <= latest_version)
                console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
            else if (retries == 0)
                location.reload(true);
            else {
                console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
                ajax(version, retries - 1, timeout * 2);
            }
        });
    xhttp.open("GET", "data.txt", true);
    xhttp.responseType = "text";
    xhttp.timeout = timeout;
    xhttp.send();
}

ajax(1, 10, 100);

// process('{"refresh":0, "grp_a":"g1", "grp_b":"g2", "grp_a_round":"1,2,3,4,5", "grp_b_round":"4,5,6,7,8",' +
//    '"grp_a_skills":"4,3,2,5,6,7,3", "grp_b_skills":"2,3,5,7,5,4,3", "grp_a_dist":"1,2;4,3;6,7", "grp_b_dist":"3,4;5,1;8,7", ' +
//    '"grp_a_score":"3", "grp_b_score":"0"}');
