//
//  RigablueTestTableViewController.h
//  RigablueTest
//
//  Created by Eric Stutzenberger on 4/22/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "Rigablue/RigLeDiscoveryManager.h"
#import "Rigablue/RigAvailableDeviceData.h"
#import "Rigablue/RigLeConnectionManager.h"

@interface RigablueTestTableViewController : UITableViewController <UIAlertViewDelegate>
- (IBAction)didPressDiscoveryButton:(id)sender;
- (IBAction)didPressClearButton:(id)sender;

@end
