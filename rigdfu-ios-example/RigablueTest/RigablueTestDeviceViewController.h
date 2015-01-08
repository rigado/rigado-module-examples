//
//  RigablueTestDeviceVewControllerViewController.h
//  RigablueTest
//
//  Created by Eric P. Stutzenberger on 7/14/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "Rigablue/RigLeBaseDevice.h"

@interface RigablueTestDeviceViewController : UIViewController
@property (weak, nonatomic) IBOutlet UIButton *updateFirmwarePressed;
@property (weak, nonatomic) IBOutlet UIProgressView *updateProgress;
@property (weak, nonatomic) IBOutlet UILabel *updateStatus;
@property (weak, nonatomic) IBOutlet UITextField *sendTextField;
@property (weak, nonatomic) IBOutlet UITextField *receiveTextField;
- (IBAction)sendButtonPressed:(id)sender;
- (IBAction)backPressed:(id)sender;
- (IBAction)firmwareUpdatePressed:(id)sender;
- (void)setDevice:(RigLeBaseDevice*)device;
@end
