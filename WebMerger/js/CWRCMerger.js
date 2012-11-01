function CWRCMerger(id, updateFileTable){
    var _this = this;
    
    this.viewModel = {
        matches: new Array(),
        files: [null],
        isMerging: ko.observable(false),
        totalThreads: 3
    };
        
    this.attributes = {
        id: id,
        code: 'org.ualberta.arc.mergecwrc.ui.applet.MergerControllerApplet',
        width: 1,
        height: 1
    };
        
    this.mergeComplete = function(){
        this.viewModel.files = new Array();
        this.viewModel.baseData = new Array();
    
        updateFileTable();
    
        _this.viewModel.isMerging(false);
    };
        
    this.removeFileRow = function(index){
        _this.viewModel.files.splice(index);
        updateFileTable();
    };
        
    this.addFile = function(evt){
        var files = evt.target.files;
                
        if(window.File && window.FileReader && window.FileList && window.Blob){
            for(var i = 0, f; f = files[i]; ++i){
                _this.viewModel.files[0] = f;
            }
            
            updateFileTable();
        }else{
            alert('The file APIs are not fully supported on this browser.');
        }
    };
        
    this.mergeFiles = function(fileType){     
        _this.viewModel.isMerging(true);
    
        var loading = _this.viewModel.files.length;
        var applet = $('#' + id)[0];
                
        for(var i = 0, f; f = _this.viewModel.files[i]; ++i){
            var fileName = 'File_' + i;
            applet.startFile(fileName);
            var reader = new FileReader();
                
            reader.onload = function(result){
                //Read the string data in blocks
                var chunks = result.target.result.match(/.{1,16384}/g);
                for(chunk in chunks){
                    applet.addToFile(fileName, chunks[chunk]);
                }
                
                //Close the file
                applet.endFile(fileName);
                --loading;
                            
                _this.checkSendMerge(loading, fileType);
            };
                    
            reader.onerror = function(error){
                alert('An error occurred while trying to load the files. Please verify that your browser allows uploading of files. Error : ' + error.message);
            };
                    
            reader.readAsText(f);
        }
    };
            
    this.checkSendMerge = function(loading, fileType){
        if(loading == 0){
            //_this.viewModel.baseData = files;
            $('#' + _this.attributes.id)[0].runMerge(_this.viewModel.files.length, fileType, true, _this.viewModel.totalThreads);
        }
    };
        
    this.newEntity = function(multipleMatchId){
        if(confirm('Are you sure you wish to create a new entry to this element?')){
            $('#' + id)[0].newEntity(multipleMatchId);
        }
    };

    this.mergeEntity = function(multipleMatchId, matchId){
        if(confirm('Are you sure you wish to merge these elements?')){
            $('#' + id)[0].mergeEntity(multipleMatchId, matchId);
        }
    };
        
    this.checkStatus = function(){
        return $.parseJSON($('#' + _this.attributes.id)[0].getProgress());
    };
    
    return this;
}

/**
 * cwrcMerger - Global variable name for the cwrcMerger object.
 */
function launchApplet(cwrcMerger, consoleFunction, reportFunction, mergeChangeFunction, initializedFunction, cwrcUrl){
    var parameters = {
        jnlp_href: 'MergerControllerApplet1.1.jnlp',
        consoleFunction: consoleFunction.name,
        reportFunction: reportFunction.name,
        mergeChangeFunction: mergeChangeFunction.name,
        initializedFunction: initializedFunction.name,
        completedFunction: cwrcMerger + ".mergeComplete",
        cwrcUrl: cwrcUrl
    }
    
    deployJava.runApplet(self[cwrcMerger].attributes, parameters, '1.6');
}

var cwrcServer = "http://192.168.253.128:3000";

function updateFileTable(){
    $("#filesTable tbody tr").each(function(i, row){
        $(row).remove();
    });
    
    for(var i = 0, f; f = cwrcMerge.viewModel.files[i]; ++i){
        var row = $("<tr></tr>");
        $(row).append("<td></td>").text(f.name);
        $(row).append("<td></td>").append("<a href='javascript:removeFileRow(" + i + ")'>Remove</a>");
        
        $("#filesTable tbody").append(row);
    }
}
            
function writeConsole(text){
    $("#consoleOut").val($("#consoleOut").val() + text);
}

function writeReport(text){
    $("#reportOut").val($("#reportOut").val() + text);
}
            
// adds all merge change values to the dropdown.
function readMergeChange(jsonText){
    var currentIndex = 0;
    var viewModel = cwrcMerge.viewModel;
    viewModel.matches = $.parseJSON(jsonText);
                
    $("#mergeMatches option").each(function(i, option){
        $(option).remove();
    });
                
    for(var index = 0; index < viewModel.matches.length; ++index){
        $("#mergeMatches").append("<option value='" + index + "'>" + escapeHtml(viewModel.matches[index].name) + "</option>");
    }
    
    if(currentIndex >= viewModel.matches.length){
        currentIndex = viewModel.matches.length - 1;
    }
                
    getPossibleMatch(currentIndex);
}
            
function getPossibleMatch(index){
    var viewModel = cwrcMerge.viewModel;
    var currentIndex = 0;
    
    $("#possibleMatches option").each(function(i, option){
        $(option).remove();
    });
    
    if(index >= 0){
        var possibleMatches = viewModel.matches[index].possibleMatches;
                
        for(var index = 0; index < possibleMatches.length; ++index){
            $("#possibleMatches").append("<option value='" + index + "'>" + escapeHtml(possibleMatches[index].name) + "</option>");
        }
    }else{
        currentIndex = -1;
        $("#merges").html('');
    }
    
    setPossibleMatch(currentIndex);
}
            
function setPossibleMatch(index){
    if(index >= 0){
        var match = cwrcMerge.viewModel.matches[parseInt($("#mergeMatches option:selected").first().val())].possibleMatches[index];
        $("#merges").html('');
        $("#matches").html('');
        
        for(node in match.node){
            var diff = match.node[node];
            
            if(diff.isDifference == "true"){
                var span = $("<span style='background-color: red; color: white;'></span>")
                span.append(diff.oldText);
                $("#merges").append(span);
                
                span = $("<span style='background-color: green; color: white;'></span>")
                span.append(diff.newText);
                $("#matches").append(span);
            }else{
                var span = $("<span style='background-color: white; color: black;'></span>")
                span.append(diff.text);
                $("#merges").append(span);
                
                span = $("<span style='background-color: white; color: black;'></span>")
                span.append(diff.text);
                $("#matches").append(span);
            }
        }
    }else{
        $("#matches").html('');
    }
}
            
function escapeHtml(text){
    return $("<div/>").text(text).html();
}
            
function appLoadComplete(){
    cwrcMerge.checkStatus();
}

var cwrcMerge = new CWRCMerger('testApplet', updateFileTable)
launchApplet('cwrcMerge',
    writeConsole,
    writeReport,
    readMergeChange,
    appLoadComplete,
    cwrcServer );

$(document).ready(function(){
    $('#fileInput').change(cwrcMerge.addFile);
        
    ko.applyBindings(cwrcMerge.viewModel);
});