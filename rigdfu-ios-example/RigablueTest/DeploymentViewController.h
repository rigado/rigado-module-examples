//
//  DeploymentViewController.h
//  RigablueTest
//
//  Created by Eric P. Stutzenberger on 9/29/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DeploymentViewController : UIViewController

@property (weak, nonatomic) IBOutlet UIButton *beingDeploymentButton;
@property (weak, nonatomic) IBOutlet UILabel *deploymentStatus;
@property (strong, nonatomic) IBOutlet UIProgressView *deploymentProgress;
@property (weak, nonatomic) IBOutlet UIPickerView *deploymentPicker;
@property (weak, nonatomic) IBOutlet UILabel *deviceNameLabel;
@property (weak, nonatomic) IBOutlet UILabel *firmwareVersionLabel;
@property (weak, nonatomic) IBOutlet UILabel *mfgNameLabel;

- (IBAction)beginDeploymentPressed:(id)sender;

@end
