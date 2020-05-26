package com.example.ar_test_01;

import android.content.Context;
import android.view.Display;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class AR_Node extends AnchorNode {

    private AugmentedImage image;
    private static CompletableFuture<ModelRenderable> modelRenderableCompletableFuture;

    public AR_Node(Context context, int modelID){
        if(modelRenderableCompletableFuture==null){
            modelRenderableCompletableFuture=ModelRenderable.builder().setRegistryId("my_model")
                    .setSource(context,modelID).build();
        }
    }

    public AugmentedImage getImage() {
        return image;
    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(final AugmentedImage image) {
        this.image = image;
        if (!modelRenderableCompletableFuture.isDone()) {
            CompletableFuture.allOf(modelRenderableCompletableFuture)
                    .thenAccept((Void aVoid) -> {
                        setImage(image);
                    })
                    .exceptionally(throwable -> null);
        }
        setAnchor(image.createAnchor(image.getCenterPose()));
        float imageWidth = image.getExtentX();
        float imageHeight = image.getExtentZ();
        Node node = new Node();


        Vector3 localPosition = new Vector3(0.0f, 0.0f, 0.0f);

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.0f);

        node.setParent(this);
        node.setLocalPosition(localPosition);
        node.setLocalScale(new Vector3(imageWidth,((imageWidth+imageHeight)/2),imageHeight));
        node.setLocalRotation(new Quaternion(pose.qx(),pose.qy(),pose.qz(),pose.qw()));
        node.setRenderable(modelRenderableCompletableFuture.getNow(null));


        //localPosition.set(0.01f * image.getExtentX(), 0.01f, 0.01f * image.getExtentZ());
        //node = new Node();
        //node.setParent(this);
        //node.setLocalPosition(localPosition);
        //node.setLocalScale(new Vector3(0.01f,0.01f,0.01f));

    }

}