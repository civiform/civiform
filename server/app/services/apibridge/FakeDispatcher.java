// package services.apibridge;
//
// import java.security.NoSuchAlgorithmException;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.CompletionStage;
//
// import models.BridgeDefinition;
// import services.applicant.ApplicantData;
//
// public class FakeDispatcher implements IDispatcher {
//  @Override
//  public CompletionStage<ApplicantData> dispatch(
//      String hostUri,
//      String uriPath,
//      String version,
//      ApplicantData applicantData,
//      BridgeDefinition bridgeDefinition)
//      throws NoSuchAlgorithmException {
//    return CompletableFuture.completedFuture(applicantData);
//  }
// }
