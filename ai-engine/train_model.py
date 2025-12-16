import torch
import torch.nn.functional as F
from torch_geometric.nn import SAGEConv
from torch_geometric.loader import DataLoader
import os

# CONFIG
SHARED_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "shared-data")
MODEL_PATH = os.path.join(SHARED_DATA_DIR, "mule_model.pth")

# 1. DEFINE THE BRAIN (Graph Neural Network)
class MuleSAGE(torch.nn.Module):
    def __init__(self, in_channels, hidden_channels, out_channels):
        super(MuleSAGE, self).__init__()
        # Layer 1: Look at immediate neighbors (1-Hop)
        # SAGEConv is the "GraphSAGE" layer
        self.conv1 = SAGEConv(in_channels, hidden_channels)
        
        # Layer 2: Look at neighbors' neighbors (2-Hop)
        self.conv2 = SAGEConv(hidden_channels, out_channels)

    def forward(self, x, edge_index):
        # Step 1: Aggregate info from neighbors
        x = self.conv1(x, edge_index)
        x = F.relu(x) # Activation function (add non-linearity)
        x = F.dropout(x, p=0.5, training=self.training) # Drop data to prevent overfitting
        
        # Step 2: Aggregate again (deeper pattern recognition)
        x = self.conv2(x, edge_index)
        
        # Step 3: Output probability (Log Softmax for classification)
        return F.log_softmax(x, dim=1)

# 2. THE TRAINING LOOP
def train_gnn():
    print("🧠 Loading the Brain Food (Graph Data)...")
    data_path = os.path.join(SHARED_DATA_DIR, "processed_graph.pt")
    
    if not os.path.exists(data_path):
        raise FileNotFoundError("❌ Run feature_engineering.py first!")
        
    # weights_only=False allows loading complex objects like our Graph Data
    data = torch.load(data_path, weights_only=False)
    
    # Initialize the Model
    # Input Features = 5 (The math clues we calculated)
    # Hidden Layer = 16 (The 'thinking' neurons)
    # Output = 2 (Classes: 0=Safe, 1=Fraud)
    model = MuleSAGE(in_channels=5, hidden_channels=16, out_channels=2)
    
    # Optimizer (Adam is the standard for deep learning)
    optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)
    
    # Weighted Loss: Fraud is rare (imbalanced). 
    # We tell the model: "Missing a fraud is 5x worse than flagging a safe user."
    # This fixes the "Class Imbalance" problem.
    weights = torch.tensor([1.0, 5.0]) 
    criterion = torch.nn.NLLLoss(weight=weights)

    print("🏋️‍♂️  Training Started (Teaching the Mule Hunter)...")
    model.train()
    
    for epoch in range(101):
        optimizer.zero_grad() # Clear previous calculations
        
        out = model(data.x, data.edge_index) # Forward pass (Guess)
        loss = criterion(out, data.y) # Calculate Error (Loss)
        
        loss.backward() # Backward pass (Learn)
        optimizer.step() # Update weights
        
        if epoch % 10 == 0:
            # Calculate Accuracy
            pred = out.argmax(dim=1)
            correct = (pred == data.y).sum()
            acc = int(correct) / int(data.num_nodes)
            print(f"   Epoch {epoch:03d}: Loss {loss.item():.4f} | Accuracy: {acc:.4f}")

    # 3. SAVE THE MODEL
    torch.save(model.state_dict(), MODEL_PATH)
    print(f"✅ SUCCESS! Trained model saved to: {MODEL_PATH}")
    print("   The Brain is ready to be deployed.")

if __name__ == "__main__":
    train_gnn()
    