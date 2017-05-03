var UFS = (function() {
  var processId;
  var feedbackId

  function init(fid, pid, initialRating) {
    feedbackId = fid;
    processId = pid;

    var starRatingOptions =  {
      theme : 'fontawesome-stars-o',
      allowEmpty : true,
      readonly : true
    };

    if (initialRating) {
      starRatingOptions.initialRating = initialRating;
    }

    $('#totalRating').barrating(starRatingOptions);
  }

  function back() {
    window.location = 'feedbackOverview?pid=' + processId;
  }

  function deleteFeedback() {
    console.log('Delete feedback ' + feedbackId + '.');
  }

  return {
    init : init,
    back : back,
    delete : deleteFeedback
  };
})();
