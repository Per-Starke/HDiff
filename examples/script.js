$(document).ready(function(){
    $('.edited').on("mouseenter", function(e){
        $('#explanation').html($(this).attr("popup"));
    });
    $('del').on("mouseenter", function(e){
        $('#explanation').html("<ul><li>This text is deleted!</li></ul>");
    });
    $('ins').on("mouseenter", function(e){
        $('#explanation').html("<ul><li>This text is inserted!</li></ul>");
    });

});
